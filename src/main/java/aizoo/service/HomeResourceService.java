package aizoo.service;

import aizoo.Client;
import aizoo.common.JobStatus;
import aizoo.domain.SlurmAccount;
import aizoo.repository.ExperimentJobDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HomeResourceService {
    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    private Client client;

    private static final Logger logger = LoggerFactory.getLogger(HomeResourceService.class);

    /**
     * 获取集群的资源使用情况
     * @param slurmAccount slurm的账户
     * @return 包含集群资源使用情况的map
     * 该map中，key为data，值为包含坐标图信息的map
     * 坐标图信息map中有两组值
     * 第一组key为x，值为map（有两组值，第一组key为name，值为GPU节点，第二组key为data，值为nodelist)
     * 第二组key为y,值为存放了3个map的list,每个map都包含两组数据，key分别为name和data（CPU、GPU、内存信息）
     * @throws Exception
     */
    public Map<String,Object> getClusterResourceUsedInfo(SlurmAccount slurmAccount) throws Exception {
        logger.info("Start get Cluster ResourceUsedInfo");
        Map<String,Object> rst = new HashMap<>();
        //记录坐标图的x、y轴信息
        logger.info("记录坐标图的x、y轴信息");
        Map<String,Object> coordinateInfo = new HashMap<>();
        //从slurm获取到的资源使用信息
        logger.info("从slurm获取到的资源使用信息");
        Map<String, Object> homepageInfo = client.getHomepageInfo(slurmAccount);
        //1.分别计算出每个GPU节点上各类资源利用率并加入各自的list中
        logger.info("分别计算出每个GPU节点上各类资源利用率并加入各自的list中");
        List<Double> cpuData = new ArrayList<>();
        List<Double> gpuData = new ArrayList<>();
        List<Double> memData = new ArrayList<>();
        for(Map.Entry entrys : homepageInfo.entrySet()){
            Map<String, Double> map = (Map<String, Double>) entrys.getValue();
            cpuData.add(100*map.get("CPUAlloc")/map.get("CPUTot"));
            gpuData.add(100*map.get("GresUsed")/map.get("Gres"));
            memData.add(100*(map.get("RealMemory")-map.get("FreeMem"))/map.get("RealMemory"));
        }
        //2.将得到的信息进行处理加入到记录坐标图x,y轴信息的coordinateInfo中
        //key为x表示X轴，value为记录gpu节点数据的map
        logger.info("将得到的信息进行处理加入到记录坐标图x,y轴信息的coordinateInfo中");
        List<String> nodeList = client.getNodeList(slurmAccount);
        coordinateInfo.put("x",new HashMap<String,Object>(){
            {
                put("name","GPU节点");
                put("data", nodeList);
            }
        });
        //yAxisData为存储map类型的list，包含了从slurm中获取的CPU、GPU、内存数据
        List<Map<String,Object>> yAxisData = new ArrayList<>();
        yAxisData.add(new HashMap<String,Object>(){
            {
                put("name","CPU");
                put("data",cpuData);
            }
        });
        yAxisData.add(new HashMap<String,Object>(){
            {
                put("name","GPU");
                put("data",gpuData);
            }
        });
        yAxisData.add(new HashMap<String,Object>(){
            {
                put("name","内存");
                put("data",memData);
            }
        });
        //将yAxisData作为y轴信息放入
        coordinateInfo.put("y", yAxisData);
        rst.put("data", coordinateInfo);
        logger.info("集群的资源使用情况: {}",rst.toString());
        logger.info("End get Cluster ResourceUsedInfo");
        return rst;
    }

    /**
     * 获取近五个月，每个月用户的实验成功次数和实验失败次数
     * @param username 用户姓名
     * @return 返回map，key为data,值为包含坐标轴信息的map
     * map中第一个key是x，值为月份
     * map中第二个key是y,值为包含两个map的list
     * 该list中第一个map有两组数据，第一组key为name，值为“实验成功次数”，第二组key为value，值为具体次数。
     * 该list中第二个map有两组数据，第一组key为name，值为“实验失败次数”，第二组key为value，值为具体次数。
     */
    public Map<String,Object> getScheduleJobsInfo(String username){
        logger.info("Start get ScheduleJobsInfo");
        logger.info("username: {}",username);
        Map<String,Object> rst = new HashMap<>();
        //记录坐标图的x、y轴信息
        logger.info("记录坐标图的x、y轴信息");
        Map<String,Object> coordinateInfo = new HashMap<>();
        //获取日期-成功次数和日期-失败次数的列表
        List<Map<String, Object>> successList = experimentJobDAO.countByMonthAndJobStatus(username, JobStatus.COMPLETED.toString());
        List<Map<String, Object>> failureList = experimentJobDAO.countByMonthAndJobStatus(username, JobStatus.FAILED.toString());
        //将两个map类型的List处理成3个List
        List<String> date = new ArrayList<>();
        List<Integer> successNum = new ArrayList<>();
        List<Integer> failureNum = new ArrayList<>();
        for (int i = successList.size()-1; i >=0; i--) {
            date.add((String) successList.get(i).get("date_time"));
            successNum.add(Integer.valueOf(successList.get(i).get("count_num").toString()));
            failureNum.add(Integer.valueOf(failureList.get(i).get("count_num").toString()));
        }
        //横坐标是月份
        coordinateInfo.put("x",new HashMap<String,Object>(){
            {
                put("name","月份");
                put("data", date);
            }
        });
        //纵坐标为实验成功、失败的次数
        List<Map<String,Object>> yAxisData = new ArrayList<>();
        yAxisData.add(new HashMap<String,Object>(){
            {
                put("name","实验成功次数");
                put("data",successNum);
            }
        });
        yAxisData.add(new HashMap<String,Object>(){
            {
                put("name","实验失败次数");
                put("data",failureNum);
            }
        });
        //将yAxisData作为y轴信息放入
        logger.info("将yAxisData作为y轴信息放入");
        coordinateInfo.put("y", yAxisData);
        rst.put("data", coordinateInfo);
        logger.info("近五个月，每个月用户的实验成功次数和实验失败次数结果: {}",rst.toString());
        logger.info("End get ScheduleJobsInfo");
        return rst;
    }
}
