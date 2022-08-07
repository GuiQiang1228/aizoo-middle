package aizoo.controller;

import aizoo.Client;
import aizoo.aspect.WebLog;
import aizoo.common.exception.IpNotMatchException;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.UserDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@BaseResponse
public class ClusterController { //查询服务器集群信息的相关接口

    @Autowired
    Client client;

    @Autowired
    UserDAO userDAO;

    /**
     * 获取项目中所有的gpu服务器的 hostname列表
     *
     * @param principal 用户登录信息
     * @return ResponseResult，其中 "data"键的值是一个List<String>类型的列表，保存gpu节点列表
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/gpuInfo", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "查询系统支持的gpu服务器hostname")
    public ResponseResult getGpuInfo(Principal principal) throws Exception {
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        try {
            List<String> rst = client.getGpuList(slurmAccount);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), rst);
        } catch (NoSlurmAccountException e) {
            return new ResponseResult(ResponseCode.SLURM_ACCOUNT_NULL.getCode(), "当前用户无slurm账户，无法查看集群GPU信息", null);
        } catch (IpNotMatchException e) {
            return new ResponseResult(ResponseCode.IP_NOT_MATCH.getCode(), "slurmAccount ip为空，无法查看GPU信息", null);
        }
    }

    /**
     * 查询GPU服务器节点的使用信息
     *
     * @param number 每台GPU服务器节点查询的GPU数量
     * @param gpuList 需要查询的GPU服务器节点的列表
     * @param principal 用户登录信息
     * @return ResponseResult，其中 "data"键的值是一个List< Map<String, Object> >类型的列表，
     *         其中 Map<String, Object>，格式为：{"name":"节点的名称", "data":"节点的使用情况"}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/gpuStatus", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "查询gpu相关使用信息")
    public ResponseResult getGpuStatus(@RequestParam(value = "number", required = false, defaultValue = "1") Integer number,
                                  @RequestParam(value = "gpuList", required = false, defaultValue = "") List<String> gpuList, Principal principal) throws Exception {
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        try{
            List<Map> rst = client.getGpuStatus(number, gpuList, slurmAccount);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), rst);
        } catch (NoSlurmAccountException e) {
            return new ResponseResult(ResponseCode.SLURM_ACCOUNT_NULL.getCode(), "无Slurm账号,无法查询gpu相关使用信息", null);
        }catch (IpNotMatchException e) {
            return new ResponseResult(ResponseCode.IP_NOT_MATCH.getCode(), "slurmAccount ip为空，无法查看GPU信息", null);
        }

    }
}
