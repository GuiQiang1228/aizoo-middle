package aizoo;


import aizoo.common.exception.IpNotMatchException;
import aizoo.common.exception.JobKeyEmptyException;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.SlurmAccount;
import aizoo.grpc.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final ManagedChannel channel;
    private final graphGrpc.graphBlockingStub blockingStub;


    private final Map<String, ManagedChannel> ip2GrpcChannel = new HashMap<>();
    private final Map<String, slurmGrpc.slurmBlockingStub> ip2SlurmBlockingStub2 = new HashMap<>();

    @Value("${grpc.request.time.deadline}")
    private int deadlineMs;


    @Autowired
    ObjectMapper objectMapper;

    private final String JOB_KEY_SEPARATOR = "-";


    public Client(@Value("${grpc.interpreter.addr}") String rpcHost1, @Value("${grpc.slurm.addr}") String slurmGrpcHosts,
                  @Value("${grpc.interpreter.port}") int rpcPort1, @Value("${grpc.slurm.port}") String slurmGrpcPorts) {

        channel = ManagedChannelBuilder.forAddress(rpcHost1, rpcPort1)
                .usePlaintext(true)
                .build();
        blockingStub = graphGrpc.newBlockingStub(channel);

        String[] hosts = slurmGrpcHosts.split(",");
        String[] ports = slurmGrpcPorts.split(",");
        for (int i = 0; i < hosts.length; i++) {
            String ip = hosts[i];
            int port = Integer.parseInt(ports[i]);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port)
                    .usePlaintext(true)
                    .build();
            ip2GrpcChannel.put(ip, channel);
            ip2SlurmBlockingStub2.put(ip, slurmGrpc.newBlockingStub(channel));
        }
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        for (ManagedChannel channel : ip2GrpcChannel.values()) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public Map<String, Object> interpretExperiment(String graph, String job, String savePath, String codeFilePath) {
        interpreter_experiment_request request = interpreter_experiment_request.newBuilder().setGraph(graph).setJob(job).setSavePath(savePath).setCodeFilePath(codeFilePath).build();
        interpreter_experiment_reply response;
        response = blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).interpreterExperiment(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("savePath", response.getSavePath());
        map.put("codeFilePathsList", response.getCodeFilePathsList());
        return map;
    }


    public String experimentJobStart(String args, SlurmAccount slurmAccount) throws Exception {
        logger.info("start experiment job...");
        slurmAccountAvailable(slurmAccount);
        String account = objectMapper.writeValueAsString(slurmAccount);
        String ip = slurmAccount.getIp();
        slurm_start_request request = slurm_start_request.newBuilder().setArgs(args).setSlurmAccount(account).build();
        slurm_start_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).experimentJobStart(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("start experiment job the job ID: " + response.getJobKey());
        String jobKey = response.getJobKey();
        return reformatJobKey(slurmAccount, jobKey);
    }


    public String slurmStop(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("will try to stop slurm job...");
        slurmAccountAvailable(slurmAccount);
        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        slurm_request request = slurm_request.newBuilder().setJobKey(arr[0]).build();
        slurm_stop_reply response = ip2SlurmBlockingStub2.get(arr[1]).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).slurmStop(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("slurm has stop the job (0 or 1):" + response.getStop());
        return response.getStop();


    }

    public String slurmGetNodeList(SlurmAccount slurmAccount) throws Exception {
        logger.info("will try to slurmGetNodeList ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();

        slurm_request request = slurm_request.newBuilder().build();
        slurm_get_node_list_reply response;
        response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).slurmGetNodeList(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getNodeList, nodelist={}", response.getNodeDict());
        return response.getNodeDict();
    }

    /**
     * 获取job状态，不支持多个job同时获得
     *
     * @param jobKey
     * @param slurmAccount
     * @return
     * @throws Exception
     */
    public String slurmShowJob(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("will try to slurmShowJob ...");
        slurmAccountAvailable(slurmAccount);

        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        String jobKeyForSlurm = arr[0];
        slurm_show_job_reply response;
        slurm_request request = slurm_request.newBuilder().setJobKey(jobKeyForSlurm).build();
        response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).slurmShowJob(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        String jobInfo = response.getJob();
        logger.info("jobInfo={}", jobInfo);
        // 这里，需要提前把jobkey换成带ip的jobkey
        Map<String, Object> map = objectMapper.readValue(jobInfo, new TypeReference<Map<String, Object>>() {
        });
        Object info = map.get(jobKeyForSlurm);
        map.remove(jobKeyForSlurm);
        map.put(jobKey, info);
        return objectMapper.writeValueAsString(map);
    }

    /**
     * 仅允许请求单个jobkey
     *
     * @param jobKey
     * @param slurmAccount
     * @return
     * @throws Exception
     */
    public String getJobUsage(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getJobUsage  ...");
        slurmAccountAvailable(slurmAccount);
        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        String jobKeyForSlurm = arr[0];
        slurm_request request = slurm_request.newBuilder().setJobKey(jobKeyForSlurm).build();
        slurm_show_job_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getUsageRate(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getJobUsage");

        String jobUsageStr = response.getJob();
        Map<String, List> newJobUsage = objectMapper.readValue(jobUsageStr, new TypeReference<Map<String, List>>() {
        });
        List info = newJobUsage.get(jobKeyForSlurm);
        newJobUsage.remove(jobKeyForSlurm);
        newJobUsage.put(jobKey, info);
        return objectMapper.writeValueAsString(newJobUsage);

    }

    public String getJobSummary(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getEff  ...");
        slurmAccountAvailable(slurmAccount);

        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        String jobKeyForSlurm = arr[0];
        slurm_request request = slurm_request.newBuilder().setJobKey(jobKeyForSlurm).build();
        slurm_show_job_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getSummary(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getEff");
        String jobSummaryStr = response.getJob();
        Map<String, Object> newJobSummary = objectMapper.readValue(jobSummaryStr, new TypeReference<Map<String, Object>>() {
        });
        Object info = newJobSummary.get(jobKeyForSlurm);
        newJobSummary.remove(jobKeyForSlurm);
        newJobSummary.put(jobKey, info);
        return objectMapper.writeValueAsString(newJobSummary);

    }

    public String interpretComponent(String graph, String path) {
        logger.info("Will try interpretComponent  ...");
        interpreter_component_request request = interpreter_component_request.newBuilder().setGraph(graph).setPath(path).build();
        interpreter_component_reply response = blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).interpreterComponent(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("interpretComponent");
        return response.getProcess();
    }

    public List<String> getGpuList(SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getGpuList  ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();
        slurm_request request = slurm_request.newBuilder().build();
        get_gpu_list_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getGpuList(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getGpuList");
        return response.getGpuListList();
    }

    public List<Map> getGpuStatus(Integer number, List<String> nodeList, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getGpuStatus  ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();
        get_gpu_status_request request = get_gpu_status_request.newBuilder().setNumber(number).setGpuList(nodeList.toString()).build();
        get_gpu_status_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getGpuStatus(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getGpuStatus");
        String totalUsage = response.getTotalUsage();
        Map<String, List<String>> res = null;
        try {
            res = objectMapper.readValue(totalUsage, new TypeReference<Map<String, List<String>>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        List<Map> resList = new ArrayList<>();
        for (String nodeName : res.keySet()) {
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("name", nodeName);
            contentMap.put("data", res.get(nodeName));
            resList.add(contentMap);
        }
        return resList;
    }

    public Map<String, Object> getHomepageInfo(SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getHomepageInfo  ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();
        get_homepage_info_request request = get_homepage_info_request.newBuilder().build();
        get_homepage_info_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getHomepageInfo(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getHomepageInfo");
        String resourceInfo = response.getResourceInfo();
        Map<String, Object> rst = objectMapper.readValue(resourceInfo, new TypeReference<Map<String, Object>>() {
        });
        return rst;
    }

    public String[] interpretService(String graph, String path) {
        logger.info("Will try interpretService  ...");
        interpreter_service_request request = interpreter_service_request.newBuilder().setGraph(graph).setPath(path).build();
        interpreter_service_reply response = blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).interpreterService(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("interpretService");
        return new String[]{response.getPath(), response.getSavePath()};
    }

    public String serviceStart(String args, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try serviceStart  ...");
        slurmAccountAvailable(slurmAccount);
        String accountJsonStr = objectMapper.writeValueAsString(slurmAccount);
        String ip = slurmAccount.getIp();
        service_request request = service_request.newBuilder().setArgs(args).setSlurmAccount(accountJsonStr).build();
        service_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).serviceStart(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("serviceStart");
        String jobKey = response.getJobKey();
        return reformatJobKey(slurmAccount, jobKey);
    }

    public String serviceStop(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try serviceStop  ...");
        slurmAccountAvailable(slurmAccount);

        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        String jobKeyForSlurm = arr[0];

        slurm_request request = slurm_request.newBuilder().setJobKey(jobKeyForSlurm).build();
        slurm_stop_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).slurmStop(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("serviceStop");
        return response.getStop();
    }

    public Map<String, String> getServiceIpAndPort(String jobKey, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getServiceIpAndPort  ...");
        slurmAccountAvailable(slurmAccount);

        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        String jobKeyForSlurm = arr[0];
        get_job_ip_port_request request = get_job_ip_port_request.newBuilder().setJobId(jobKeyForSlurm).build();
        get_job_ip_port_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getJobIpPort(request);

        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }

        Map<String, String> serviceReply = new HashMap<>();
        serviceReply.put("ip", response.getIp());
        serviceReply.put("port", response.getPort());
        return serviceReply;
    }

    public String[] interpretApplication(String graph, String job, String path, String codeDir) {
        logger.info("Will try interpretApplication  ...");
        interpreter_application_request request = interpreter_application_request.newBuilder().setGraph(graph).setJob(job).setPath(path).setCodeDir(codeDir).build();
        interpreter_application_reply response = blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).interpreterApplication(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("interpretApplication");
        return new String[]{response.getPath(), response.getSavePath()};
    }

    public String applicationStart(String args, SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try applicationStart  ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();
        String account = objectMapper.writeValueAsString(slurmAccount);
        application_request request = application_request.newBuilder().setArgs(args).setSlurmAccount(account).build();
        get_application_start_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).applicationStart(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("applicationStart done");
        String jobKey = response.getJobKey();
        return reformatJobKey(slurmAccount, jobKey);
    }

    public List<String> getNodeList(SlurmAccount slurmAccount) throws Exception {
        logger.info("Will try getNodeList  ...");
        slurmAccountAvailable(slurmAccount);
        String ip = slurmAccount.getIp();
        get_node_list_request request = get_node_list_request.newBuilder().build();
        get_node_list_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getNodeList(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("getNodeList done");
        return objectMapper.readValue(response.getNodeList(), ArrayList.class);
    }

    /**
     * 根据数据库里的jobkey（拼接ip的），获取原始的jobkey和ip
     *
     * @param originJobKey
     * @return
     * @throws JobKeyEmptyException
     * @throws IpNotMatchException
     */
    private String[] getJobKeyAndIpForSlurm(String originJobKey) throws JobKeyEmptyException, IpNotMatchException {
        String[] arr = originJobKey.split(JOB_KEY_SEPARATOR);
        if (arr.length <= 1) {
            logger.error("getJobKeyAndIpForSlurm failed! OriginJobKey is empty or not available! originJobKey={}", originJobKey);
            throw new JobKeyEmptyException();
        } else if (!ip2SlurmBlockingStub2.containsKey(arr[1])) {
            logger.error("getJobKeyAndIpForSlurm failed! OriginJobKey ip not available! originJobKey={}, ip={}", originJobKey, arr[1]);
            throw new IpNotMatchException();
        }
        return arr;
    }

    /**
     * slurm account 是否合法，不合法直接报错，合法则返回true
     *
     * @param slurmAccount
     * @return
     * @throws NoSlurmAccountException
     * @throws IpNotMatchException
     */
    private boolean slurmAccountAvailable(SlurmAccount slurmAccount) throws NoSlurmAccountException, IpNotMatchException {
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        String ip = slurmAccount.getIp();
        if (ip == null || !ip2SlurmBlockingStub2.containsKey(ip)) {
            throw new IpNotMatchException();
        }
        // 注意，上边如果有问题，都直接抛出了，不会return
        return true;
    }

    /**
     * 从slurm端拿的jobkey，给加上ip后缀，注意这里默认slurm account合法
     *
     * @param slurmAccount
     * @param jobKey
     * @return
     * @throws JobKeyEmptyException
     */
    private String reformatJobKey(SlurmAccount slurmAccount, String jobKey) throws JobKeyEmptyException {
        String[] arr = jobKey.split(JOB_KEY_SEPARATOR);
        if (arr.length == 0) {
            logger.error("applicationStart method getJobKey() is empty!");
            throw new JobKeyEmptyException();
        } else if (arr.length == 1) {
            jobKey = jobKey + JOB_KEY_SEPARATOR + slurmAccount.getIp();
        }
        return jobKey;
    }

    public String mirrorJobStart(String args, String userArgs, SlurmAccount slurmAccount) throws Exception {
        logger.info("start mirror job...");
        slurmAccountAvailable(slurmAccount);
        String account = objectMapper.writeValueAsString(slurmAccount);
        String ip = slurmAccount.getIp();
        slurm_mirror_start_request request = slurm_mirror_start_request.newBuilder().setArgs(args).setSlurmAccount(account).setUserArgs(userArgs).build();
        slurm_start_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).mirrorJobStart(request);
        if (Context.current().isCancelled()) {
            throw new RuntimeException();
        }
        logger.info("start mirror job the job ID: " + response.getJobKey());
        String jobKey = response.getJobKey();
        return reformatJobKey(slurmAccount, jobKey);
    }

    /**
     * 获取Job的Loss和metric
     * @param jobKey
     * @param slurmAccount
     * @param args job根目录
     * @return Job的Loss和metric
     * @throws Exception
     */
    public String getJobLossAndMetric(String jobKey, SlurmAccount slurmAccount, String args) throws Exception {
        logger.info("Will try getJobLossAndMetric  ...");
        slurmAccountAvailable(slurmAccount);

        String[] arr = getJobKeyAndIpForSlurm(jobKey);
        String ip = arr[1];
        get_job_loss_and_metric_request request = get_job_loss_and_metric_request.newBuilder().setArgs(args).build();
        get_job_loss_and_metric_reply response = ip2SlurmBlockingStub2.get(ip).withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getJobLossAndMetric(request);
        return response.getJobLossAndMetric();
    }
}
