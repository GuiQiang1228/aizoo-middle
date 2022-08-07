package ServiceTest;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public class WebSocketTest {

    @Test
    public void test(){

        String jobUsageStr = "{\"255\": []," +
                "\"256\": [{\"node\": \"centos7-sub1\", \"PID\": \"3774\", \"USER\": \"root\", \"PR\": \"20\", \"NI\": \"0\", \"VIRT\": \"113320\", \"RES\": \"1736\", \"SHR\": \"1344\", \"S\": \"S\", \"CPU\": \"0.0\", \"MEM\": \"0.1\", \"TIME+\": \"0:00.00\", \"COMMAND\": \"slurm_scr+\", \"TIME\": \"2020-06-02 10:38:36\"}, {\"node\": \"centos7-sub1\", \"PID\": \"3783\", \"USER\": \"root\", \"PR\": \"20\", \"NI\": \"0\", \"VIRT\": \"107960\", \"RES\": \"620\", \"SHR\": \"520\", \"S\": \"S\", \"CPU\": \"0.0\", \"MEM\": \"0.0\", \"TIME+\": \"0:00.00\", \"COMMAND\": \"sleep\", \"TIME\": \"2020-06-02 10:38:36\"}] }";
        int jobId = 256;
        List<Object> runData = new ArrayList<>();
        //先修改WebSocket.getRunData方法权限
//        runData = WebSocket.getRunData(jobUsageStr,jobId);
        System.out.println(runData);



    }
}
