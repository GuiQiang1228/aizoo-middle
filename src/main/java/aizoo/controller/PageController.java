package aizoo.controller;

import aizoo.aspect.WebLog;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.text.ParseException;
import java.util.Optional;

@Controller
public class PageController {

    @Autowired
    UserService userService;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    /**
     * design页面跳转，如果没有权限，返回noPermission
     *
     * @param graphId 需跳转的graph的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return design/noPermission
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/design/{graphId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "design页面跳转")
    public String getDesignPage(@PathVariable String graphId, Principal principal) {
        Graph graph = graphDAO.findById(Long.valueOf(graphId)).orElseThrow(() -> new EntityNotFoundException());
        if(!graph.getUser().getUsername().equals(principal.getName()))
            return "noPermission";
        return "design";
    }

    /**
     * Job详情页面跳转，如果没有权限，返回noPermission
     *
     * @param taskId  需跳转的experimentJob的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return task/noPermission
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/jobs/{taskId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "Job详情页")
    public String getTaskPage(@PathVariable String taskId, Principal principal) {
        ExperimentJob experimentJob = experimentJobDAO.findById(Long.valueOf(taskId)).orElseThrow(() -> new EntityNotFoundException());
        if(!experimentJob.getUser().getUsername().equals(principal.getName()))
            return "noPermission";
        return "task";
    }

    /**
     *Job下载页面跳转，如果没有权限，返回noPermission
     *
     * @param jobId
     * @param principal
     * @return download/noPermission
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/download/{jobId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "Job下载页")
    public String getJobPage(@PathVariable String jobId, Principal principal) {
        Optional<ExperimentJob> job = experimentJobDAO.findById(Long.valueOf(jobId));
        if(job.isPresent()) {
            ExperimentJob experimentJob = job.get();
            if (!experimentJob.getUser().getUsername().equals(principal.getName()))
                return "noPermission";
        }
        else {
            Optional<ServiceJob> job1 = serviceJobDAO.findById(Long.valueOf(jobId));
            if(job1.isPresent()) {
                ServiceJob serviceJob = job1.get();
                if (!serviceJob.getUser().getUsername().equals(principal.getName()))
                    return "noPermission";
            }
            else{
                Optional<Component> job2 = componentDAO.findById(Long.valueOf(jobId));
                if(job2.isPresent()){
                    Component component = job2.get();
                    if (!component.getUser().getUsername().equals(principal.getName()))
                        return "noPermission";
                }
                else{
                    Optional<MirrorJob> job3 = mirrorJobDAO.findById(Long.valueOf(jobId));
                    if(job3.isPresent()){
                        MirrorJob mirrorJob = job3.get();
                        if(!mirrorJob.getUser().getUsername().equals(principal.getName()))
                            return "noPermission";
                    }
                }
            }
        }
        return "download";
    }

    /**
     * serviceJob详情页面跳转，如果没有权限，返回noPermission
     *
     * @param serviceJobId
     * @param principal
     * @return serviceDetails/noPermission
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/service/{serviceJobId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "serviceJob详情页")
    public String getServiceJobPage(@PathVariable String serviceJobId, Principal principal) {
        ServiceJob serviceJob = serviceJobDAO.findById(Long.valueOf(serviceJobId)).orElseThrow(() -> new EntityNotFoundException());
        if(!serviceJob.getUser().getUsername().equals(principal.getName()))
            return "noPermission";
        return "serviceDetails";
    }

    /**
     * notebook 编辑页面跳转
     *
     * @return notebook
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/notebook/*", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "notebook 编辑页面")
    public String getNotebookEditPage() {
        return "notebook";
    }

    /**
     * mirrorJob详情页面跳转，如果没有权限，返回noPermission
     *
     * @param mirrorJobId
     * @param principal
     * @return mirrorTask/noPermission
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/mirrorjobs/{mirrorJobId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "mirrorJob详情页")
    public String getMirrorJobPage(@PathVariable String mirrorJobId, Principal principal) {
        MirrorJob mirrorJob = mirrorJobDAO.findById(Long.valueOf(mirrorJobId)).orElseThrow(() -> new EntityNotFoundException());
        if(!mirrorJob.getUser().getUsername().equals(principal.getName()))
            return "noPermission";
        return "mirrorTask";
    }

    /**
     * 查看文件页面跳转
     *
     * @return fileNetdisk
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/fileNetdisk/**", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "mirrorJob详情页")
    public String getFileNetdiskPage() {
        return "fileNetdisk";
    }

    /**
    * @Description: 只读文件页面跳转

    * @return: java.lang.String “fileRead”
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/fileRead/**", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "只读文件内容页面")
    public String getFileReadPage() {
        return "fileRead";
    }

}
