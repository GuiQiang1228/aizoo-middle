package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.UserDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.SlurmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@BaseResponse
@RestController
public class ComputeResourceController {

    @Autowired
    private SlurmService slurmService;

    @Autowired
    private UserDAO userDAO;

    /**
     * 根据jobId来终止作业
     *
     * @param jobKey     需终止的job的jobKey
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null}，失败状态：{code: 12000, msg: "终止失败请重试", data: null}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/cancelJob", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "终止作业")
    public ResponseResult slurmStop(@MultiRequestBody String jobKey, Principal principal) throws Exception {
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        //判断是否终止成功
        int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
        if (flag == 0)
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), "终止成功");
        else
            return new ResponseResult(ResponseCode.CANCEL_JOB_ERROR.getCode(), ResponseCode.CANCEL_JOB_ERROR.getMsg(), "终止失败");
    }

    /**
     * 获取node节点信息
     *
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return NodeList
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/showNodeList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "getNodeList")
    public ResponseResult slurmGetNodeList(Principal principal) throws Exception {
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), slurmService.getNodeList(slurmAccount));
    }

    /**
     * 根据jobkey查jobInfo并返回
     *
     * @param jobKey  格式为 {slurm中jobkey}-{本用户对应的集群ip}
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return 字符串形式返回  实际组织形式是只有一个key的map，key值为jobKey
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/showJob", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "showJob")
    public ResponseResult slurmShowJob(@MultiRequestBody String jobKey, Principal principal) throws Exception {
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), slurmService.showJob(jobKey, slurmAccount));
    }


}
