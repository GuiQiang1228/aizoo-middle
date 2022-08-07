package aizoo.controller;

import aizoo.aspect.WebLog;
import aizoo.domain.SlurmAccount;
import aizoo.service.SetSlurmAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class SetSlurmAccountController {

    @Autowired
    SetSlurmAccountService setSlurmAccountService;


    /**
     * 为名为username的用户添加slurm账户，所添加的slurm账户名为slurmAccount_username
     * @param username 用户名
     * @param slurmAccount_username slurm账户的用户名
     * @return  "no slurmAccount"表示所指定的slurmAccount_username不存在，"no user"表示所指定的username不存在，"successful"表示设置成功
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/setSlurmAccount", method = RequestMethod.GET)
    @WebLog(description = "为用户添加Slurm Account账户")
    public String setSlurmAccount(@RequestParam(value = "username", required = false, defaultValue = "") String username,
                                  @RequestParam(value = "slurmAccount_username", required = false, defaultValue = "") String slurmAccount_username,
                                  @RequestParam(value = "slurmAccount_ip", required = false, defaultValue = "") String slurmAccount_ip){
        String result = setSlurmAccountService.setSlurmAccountForUser(username, slurmAccount_username, slurmAccount_ip);
        return result;
    }

    /**
     * 从数据库中获取现有的所有SLurm账户
     * @return 所有SLurm账户组成的列表
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/getSlurmAccounts", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取现有的所有Slurm Account账户")
    public List<Map<String, String>> getSlurmAccounts(){
        List<Map<String, String>> slurmAccounts = setSlurmAccountService.getSlurmAccounts();
        return  slurmAccounts;
    }

    /**
     * 获取目前已经有Slurm账户的用户
     * @return {用户id: [slurm账户用户名, slurm账户IP], ..., }
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/getUserWithSlurmAccount", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取有Slurm Account账户的用户")
    public Map<Long, List> getUserWithSlurmAccount(){
        Map<Long, List> userWithSlurmAccount = setSlurmAccountService.getUserWithSlurmAccount();
        return  userWithSlurmAccount;
    }

}
