package aizoo.service;

import aizoo.Client;
import aizoo.common.JobStatus;
import aizoo.domain.MirrorJob;
import aizoo.domain.SlurmAccount;
import aizoo.repository.MirrorJobDAO;
import aizoo.repository.UserDAO;
import aizoo.utils.TimeUtil;
import aizoo.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/mirrorWebsocket/{jobId}")
public class MirrorWebsocket {
    private final static Logger logger = LoggerFactory.getLogger(MirrorWebsocket.class);

    protected static Map<Integer, Session> clients = new ConcurrentHashMap<>();
    protected static Set<Integer> jobList = new CopyOnWriteArraySet<>(); // job id列表
    protected static Set<String> jobKeyList = new CopyOnWriteArraySet<>(); // job key 列表
    protected static AtomicInteger onlineNumbers = new AtomicInteger(); // 线程安全的计数器，用于统计当前连接的客户端数量
    protected static List<JobStatus> stopStatus = JobStatus.stopJobStatus(); // 这些状态下，job不再执行
    protected static List<String> jobUsage2Parse = Arrays.asList("GPU", "Mem", "CPU", "RAM", "GPU_MEMORY_USAGE");// 需要解析的job资源利用率

    protected static Object lock = new Object();

    private static final int DEFAULT_LOG_LENGTH = 200;  // 默认获取日志的行数

    private static Client rpcClient;

    private static MirrorJobDAO mirrorJobDAO;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String JOB_KEY_SEPARATOR = "-";

    private static Thread jobUpdateThread = null;


    @Autowired
    public void setClient(Client client) {
        MirrorWebsocket.rpcClient = client;
    }

    @Autowired
    public void setJobMapper(MirrorJobDAO mirrorJobDAO, UserDAO userDAO) {
        MirrorWebsocket.mirrorJobDAO = mirrorJobDAO;
    }

    static {
        createJobDataUpdateThread();
        if (jobUpdateThread.getState().equals(Thread.State.NEW))
            jobUpdateThread.start();
        else {
            logger.error("MirrorWebSocket jobUpdateThread started failed！ thread state={}", jobUpdateThread.getState());
        }
    }

    /**
     * 新建个线程，线程用于遍历所有连接到websocket服务的job，更新他们的执行信息，并发送到前端
     * 注意，没有任何websocket连接时，线程将阻塞等待
     */
    private static void createJobDataUpdateThread() {
        if (jobUpdateThread == null)
            jobUpdateThread = new Thread(() -> {
                while (true) {
                    // 1. 没有链接时，阻塞并等待
                    synchronized (lock) {
                        while (clients.size() == 0) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                logger.error("wait for client connection interrupted! e={}", e);
                            }
                        }
                    }
                    // 2. 有了连接后,遍历job列表，更新job信息，推送给前端
                    for (String jobKey : jobKeyList) {
                        try {
                            sendJobData(jobKey);
                        } catch (Exception exception) {
                            logger.error("can not send job data! jobKey={}, e={}", jobKey, exception);
                        }
                    }

                    // 3. 等待3s之后，再次进行更新（或阻塞等待）操作
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        logger.error("thread sleep interrupted! e={}", e);
                    }
                }
            });
    }

    /**
     * websocket连接建立
     * 先加锁
     * 再更新 jobList、jobKeyList、保存连接会话更新连接客户端计数器
     * 最后通知job更新线程jobUpdateThread解除阻塞状态
     *
     * @param jobId   建立连接的jobId（也是客户端的唯一标识）
     * @param session 本连接对应的会话
     */
    @OnOpen
    public void onOpen(@PathParam("jobId") Integer jobId, Session session) {
        logger.info("Start onOpen");
        logger.info("onOpen jobId:{},session:{}", jobId, session);
        synchronized (lock) {
            logger.info("{} connected", jobId);
            onlineNumbers.incrementAndGet();
            // 1. 更新jobList
            jobList.add(jobId);
            // 2. 更新jobKeyList
            MirrorJob mirrorJob = mirrorJobDAO.findById(Long.parseLong(jobId.toString())).get();
            String jobKey = mirrorJob.getJobKey();
            jobKeyList.add(jobKey);
            // 3. 保存连接会话
            clients.put(jobId, session);
            // 4. jobUpdateThread解除阻塞状态
            lock.notifyAll();
        }
        logger.info("End onOpen");
    }

    /**
     * 会话关闭时操作：
     * 加锁
     * 从jobList、jobKeyList删除对应job，更新连接计数器、删除会话
     *
     * @param jobId
     */
    @OnClose
    public void onClose(@PathParam("jobId") Integer jobId) {
        logger.info("Start onClose");
        logger.info("onOpen jobId:{}", jobId);
        synchronized (lock) {
            if (jobId == null || !clients.containsKey(jobId)) return;
            onlineNumbers.decrementAndGet();
            jobList.remove(jobId);
            clients.remove(jobId);
            MirrorJob mirrorJob = mirrorJobDAO.findById(Long.parseLong(jobId.toString())).get();
            String jobKey = mirrorJob.getJobKey();
            jobKeyList.remove(jobKey);
            logger.info("{} disconnected", jobId);
        }
        logger.info("End onClose");
    }

    /**
     * 前端主动请求操作方法，本类中只用于前端主动请求某个job的日志文件特定行的日志内容
     * 发送给前端的数据格式见 Websocket.onMessage.resultMessage.md
     *
     * @param jobId
     * @param message json str，格式为{"start":起始行数,"end":终止行数,"logType":"outLog/errorLog"}
     * @throws JsonProcessingException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    @OnMessage
    public void onMessage(@PathParam("jobId") Integer jobId, String message) throws JsonProcessingException, FileNotFoundException, UnsupportedEncodingException {
        logger.info("Start onMessage");
        logger.info("onMessage jobId:{},message:{}", jobId, message);

        Map<String, Object> resultMessage = new HashMap<>();
        resultMessage.put("isLogFile", true);

        // 1. 解析前端请求
        Map<String, Object> messageMap = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {
        });
        Integer start = (Integer) messageMap.get("start");
        Integer end = (Integer) messageMap.get("end");
        String logType = (String) messageMap.get("logType");
        resultMessage.put("logType", logType);


        // 2. 根据environment和jobKey拿到真实log地址
        MirrorJob mirrorJob = mirrorJobDAO.findById(Long.parseLong(jobId.toString())).get();
        String logPath = getLogPath(mirrorJob.getEnvironment(), mirrorJob.getJobKey(), logType);


        // 3. 判断当前任务是否已经结束，本状态会被传给前端
        boolean finished = !(JobStatus.needUpdate().contains(mirrorJob.getJobStatus()));

        // 4. 组织返回数据
        try {
            Map<String, Object> logMessage = readLogFile(logPath, start, end, finished);
            resultMessage.put(logType, logMessage);
        } catch (Exception e) {
            logger.error("something wrong when reading log file, logPath={}, error={}", logPath, e);
            resultMessage.put("error", e.getMessage());
        }

        // 5. 发送数据
        Integer id = Integer.parseInt(String.valueOf(mirrorJob.getId()));
        RemoteEndpoint.Async session = clients.get(id).getAsyncRemote();
        session.sendText(objectMapper.writeValueAsString(resultMessage));
        logger.info("End onMessage");
    }


    /**
     * 读取指定行的日志文件
     * 若文件不存在，则组织空数据返回
     * start不存在或小于零时，默认从头开始，end不存在或小于start时，则默认偏移DEFAULT_LOG_LENGTH
     * 读取的数据行号包括start和end两端
     *
     * @param logFilePath 日志文件地址
     * @param start       读取起始行（从1计数），可为null
     * @param end         读取终止行（从1计数），可为null
     * @param finished    日志对应的job是否结束
     * @return Map<String, Object>格式，{"data":[{"id":行id，从1计数，"line":行号，默认与id相同，"text":本行的文本},...],"start":起始行号，"end":终止行号,"lineNum":日志文件当前行数（可能在job执行过程中继续增加）,"finished":true/false,job是否结束}
     * @throws IOException
     */
    private static Map<String, Object> readLogFile(String logFilePath, Integer start, Integer end, boolean finished) throws IOException {
        logger.info("start readLogFile. logFilePath={}, start={}, end={}, finished={}", logFilePath, start, end, finished);
        File logFile = new File(logFilePath);
        // 若文件不存在，则返回空数据
        if (!logFile.exists()) {
            return getEmptyLogMessage(start, end, finished);
        }

        // 若文件已创建：
        // lineNumberReader继承自BufferedReader，多了行号追踪功能
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(logFile));

        // 1. 根据start和end的值，设置真正要读取的文件行号
        // start不存在时，或小于0时，从头开始读
        Integer realStart = start == null || start <= 0 ? 1 : start;
        // end不存在，或end小于start时，从start往后偏移默认长度
        Integer realEnd = end == null || end - realStart < 0 ? realStart + DEFAULT_LOG_LENGTH : end;
        logger.info("realStart={}, realEnd={}", realStart, realEnd);


        // 2. 跳过前面的realStart行和realEnd后面的行，读取特定行文本，
        // 用于存放日志文件的每一行
        List<Map<String, Object>> logLines = new ArrayList<>();
        String lineStr = "";
        while ((lineStr = lineNumberReader.readLine()) != null) {
            // 给前端的数据，行号从1开始计数,lineNumberReader从0开始计数，但已经读完一行了，行号已经变到了下一行，所以不用加一
            int lineNumCount = lineNumberReader.getLineNumber();

            // realStart-1行,从realStart行开始读取，跳过从realEnd+1之后所有行，包括realEnd+1
            if (lineNumCount >= realStart && lineNumCount <= realEnd) {
                String text = lineStr.replaceAll("\u001B\\[A", "");
                logLines.add(new HashMap<String, Object>() {{
                    put("id", lineNumCount);
                    put("line", lineNumCount);
                    put("text", text);
                }});
            }
        }

        // 当前文件总行数，即lineStr=null时，即日志文件从0计数的行号，最后一行有内容的下一行
        int totalLines = lineNumberReader.getLineNumber();
        logger.info("totalLines={}", totalLines);


        // 3. 组织给前端返回的message
        Map<String, Object> logMessage = new HashMap<String, Object>() {{
            put("data", logLines);
            // 如果请求的start比当前最大行数还大，则realStart和realEnd都取null
            if (realStart > totalLines) {
                put("start", null);
                put("end", null);
            } else {
                put("start", realStart);
                // end的行号可能比总行数还大，返回的end需要取二者的最小值
                put("end", Math.min(realEnd, totalLines));
            }
            put("lineNum", totalLines);
            put("finished", finished);
        }};


        // 4. 读取完文件，需要关闭reader
        lineNumberReader.close();
        logger.info("end readLogFile. logMessage={}", logMessage);
        return logMessage;
    }

    /**
     * 空的日志数据返回值
     *
     * @param start
     * @param end
     * @param finished
     * @return
     */
    private static Map<String, Object> getEmptyLogMessage(Integer start, Integer end, boolean finished) {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> logMessage = new HashMap<String, Object>() {{
            put("data", data);
            put("start", start);
            put("end", end);
            put("lineNum", 0);
            put("finished", finished);
        }};
        return logMessage;
    }

    /**
     * 返回job的日志文件路径
     *
     * @param environment  job的environment str
     * @param originJobKey job的key（带ip的），格式： {slurm上的jobkey}-{slurm集群对应ip}
     * @param logType      哪种log （outLog/errorLog）
     * @return 对应job的某种log的真实文件地址
     * @throws JsonProcessingException
     */
    private static String getLogPath(String environment, String originJobKey, String logType) throws JsonProcessingException {
        logger.info("Start get Log Path");
        logger.info("getLogPath environment:{},originJobKey:{}, logType:{}", environment, originJobKey, logType);
        // 1. 切出原始jobKey
        String[] arr = originJobKey.split(JOB_KEY_SEPARATOR);
        String jobKeyInSlurm = arr[0];

        // 2. 解析出environment的json对象
        Map<String, Map<String, Object>> environMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        Map<String, Object> environMapInfo = environMap.get(originJobKey);

        // 3. 根据environment给的log路径，替换掉%J，变成真实的log路径
        String errorLogPath = ((String) environMapInfo.get("std_err")).replace("%J", jobKeyInSlurm);
        String outLogPath = ((String) environMapInfo.get("std_out")).replace("%J", jobKeyInSlurm);

        logger.info("getLogPath errorLogPath={},outLogPath={}", errorLogPath, outLogPath);
        logger.info("End get Log Path");
        return logType.equals("outLog") ? outLogPath : errorLogPath;
    }


    /**
     * 给websocket server连接的某个client发送一条job数据
     *
     * @param jobKey
     * @throws Exception
     */
    private static void sendJobData(String jobKey) throws Exception {
        // 1. 根据jobKey在数据库里查到对应job
        MirrorJob mirrorJob = mirrorJobDAO.findByJobKey(jobKey);

        // 2. 向slurm server发请求，查询job资源使用情况、运行统计信息、MirrorJob的IP和Port
        User user = mirrorJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        String jobUsageStr = rpcClient.getJobUsage(jobKey, slurmAccount);
        String jobSummaryStr = rpcClient.getJobSummary(jobKey, slurmAccount);
        String ip = mirrorJob.getIp();
        String port = mirrorJob.getPort();
        String userArgs = mirrorJob.getUserArgs();
        Map<String, String> newUserArgs = null;
        if (StringUtils.isNotEmpty(userArgs)) {
            newUserArgs = objectMapper.readValue(userArgs, new TypeReference<Map<String, String>>() {});
        }
        Map<String, List> newJobUsage = objectMapper.readValue(jobUsageStr, new TypeReference<Map<String, List>>() {
        });
        Map<String, Object> newJobSummary = objectMapper.readValue(jobSummaryStr, new TypeReference<Map<String, Object>>() {
        });
        // 3. 解析并组织数据
        Map<String, Object> jobData = parseJobData(mirrorJob, newJobUsage, newJobSummary, ip, port, newUserArgs);
        jobData.put("isLogFile", false);   // 本条数据不是日志文件

        // 4. 发送数据
        Integer id = Integer.parseInt(String.valueOf(mirrorJob.getId()));
        logger.info("send job data to client..id={}", id);
        RemoteEndpoint.Async session = clients.get(id).getAsyncRemote();
        String json = objectMapper.writeValueAsString(jobData);
        session.sendText(json);
    }

    /**
     * 整理并解析job的运行信息（正在执行时）、统计信息（执行完成后）以及job的基本信息（如状态、运行时间、分配到的节点等等）
     *
     * @param mirrorJob  要解析的job对象，其environment字段已经包括了该job的分配节点信息、已运行时长、最大内存等等
     * @param jobUsage   该job的资源利用率（前端只会在job未结束时展示），包括gpu利用率、cpu利用率等等，数据由slurm service返回，只有jobkey一个key值的map，组织形式见DataStructure.md
     * @param jobSummary 该job的一些统计信息（前端只会在job结束时展示），只有jobkey一个key值的map，包括运行总时长、集群名、内存占用状态等等，组织形式见DataStructure.md
     * @return Map<String, Object> 格式:
     * {
     * "showData":{数据库里的mirrorJob对象序列化之后的map，其中environment是去掉jobkey之后的子map},
     * "runData":{job运行信息，格式参考parseRunData方法，job还在运行时使用，和stopData不会同时不为空}
     * "stopData":{job运行完后的统计信息，job结束后使用，和runData不会同时不为空}
     * }
     * @throws JsonProcessingException
     */
    public static Map<String, Object> parseJobData(MirrorJob mirrorJob, Map<String, List> jobUsage, Map<String, Object> jobSummary, String ip, String port, Map<String, String> userArgs) throws JsonProcessingException {
        String jobKey = mirrorJob.getJobKey();
        logger.info("Start parseJobData,  jobKey:{}, jobUsage:{}, jobSummary:{}", jobKey, jobUsage, jobSummary);
        Map<String, Object> jobData = new HashMap<>();

        // 1. 将目标job序列化成map，填入showData
        Map<String, Object> jobMap = objectMapper.readValue(objectMapper.writeValueAsString(mirrorJob), Map.class);
        // 前端需要的environment是去掉jobkey之后的map，所以后台需要预先解析数据的environment后，将里层map放入showData.environment
        String jobEnvironment = mirrorJob.getEnvironment();
        Map<String, Object> jobEnvironmentMap = objectMapper.readValue(jobEnvironment, Map.class);
        jobMap.put("environment", jobEnvironmentMap.get(jobKey));
        if(jobEnvironmentMap.containsKey("args"))
            jobMap.put("args",jobEnvironmentMap.get("args"));
        jobMap.put("IP", ip);
        jobMap.put("Port", port);
        jobMap.put("userArgs", userArgs);
        jobData.put("showData", jobMap);


        // 2. 根据job是否在运行，放入不同的runData和stopData
        if (stopStatus.contains(mirrorJob.getJobStatus()) && jobSummary.containsKey(jobKey)) {
            // job已结束则runData为空
            jobData.put("stopData", jobSummary.get(jobKey));
            jobData.put("runData", new ArrayList<>());
        } else {
            // job还在运行则stopData为空
            jobData.put("runData", parseRunData(jobUsage.get(jobKey)));
            jobData.put("stopData", new HashMap<String, String>());
        }
        logger.info("getJobData return:{}", jobData);
        logger.info("End get Job Data");
        return jobData;

    }


    /**
     * 解析当前时间点某job的资源使用情况，按规定格式组织数据返回给前端
     *
     * @param onePieceJobUsage job的使用情况，组织形式为[{"GPU":{进程1gpu利用率},"Mem":{进程1使用的内存},...},{进程2的资源使用情况}...]
     *                         list的一个元素代表一个进程，有它自己的pid和自己的利用率
     *                         该数据来自slurm service的getJobUsage去掉外层jobKey
     * @return List<Object> 格式为参考DataStructure.md
     */
    public static List<Object> parseRunData(List<Map<String, Object>> onePieceJobUsage) {
        logger.info("start parseRunData, onePieceJobUsage:{}", onePieceJobUsage);
        List<Object> runData = new ArrayList<>();

        // 1. 获取当前时间点，避免在下层遍历并解析数据时再获取导致的时间不一致的情况
        String currentDateStr = TimeUtil.stampToDate(new Date());
        // 2. 遍历要解析的项目，分别进行解析
        for (String usageType : jobUsage2Parse) {
            Map<String, Object> data = new HashMap<>();
            // 图表名
            data.put("title", usageType.toLowerCase());

            // 图表的x轴，存放的是当前时间
            data.put("x", new HashMap<String, Object>() {{
                put("data", currentDateStr);
            }});

            // 图表的y轴，存放的是每个进程的单项利用率及进程pid
            data.put("y", new ArrayList<Map<String, Object>>() {{
                for (Map<String, Object> usageOfOneProcess : onePieceJobUsage) {
                    Map<String, Object> yData = new HashMap<String, Object>() {{
                        put("name", "PID-" + usageOfOneProcess.get("PID"));
                        put("data", usageOfOneProcess.get(usageType));
                    }};
                    add(yData);
                }
            }});
            runData.add(data);
        }
        logger.info("getRunData return:{}", runData);
        logger.info("End parseRunData");

        return runData;
    }


}
