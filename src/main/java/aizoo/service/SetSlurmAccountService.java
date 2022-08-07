package aizoo.service;

import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.SlurmAccountDAO;
import aizoo.repository.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
为用户添加Slurm账号
 */

@Service
public class SetSlurmAccountService {

    @Autowired
    UserDAO userDAO;

    @Autowired
    SlurmAccountDAO slurmAccountDAO;

    /**
     * 为用户添加slurm账号，为名为username的用户添加名为slurmAccount_username是slurm账户
     * @param username
     * @param slurmAccount_username
     * @return "no slurmAccount"表示所指定的slurmAccount_username不存在，"no user"表示所指定的username不存在，"successful"表示设置成功
     */
    public String setSlurmAccountForUser(String username, String slurmAccount_username, String ip){
        SlurmAccount slurmAccount = slurmAccountDAO.findByUsernameAndIp(slurmAccount_username, ip);
        if (slurmAccount==null) return "no slurmAccount";
        User user = userDAO.findByUsername(username);
        if (user==null){
            return "no user";
        }else {
            user.setSlurmAccount(slurmAccount);
            userDAO.save(user);
            return "successful";
        }

    }

    /**
     * 获取当前已有的Slurm账户
     * @return [{"username": Slurm账户的用户名, "ip": Slurm账户的IP}, ...]
     */
    public List<Map<String, String>> getSlurmAccounts(){
        List<SlurmAccount> slurmAccounts = slurmAccountDAO.findAll();
        List<Map<String, String>> SlurmList = new ArrayList<>();
        for(SlurmAccount slurmAccount: slurmAccounts){
            if (slurmAccount.getUsername() != null && slurmAccount.getIp() != null) {
                HashMap<String, String> map = new HashMap<>();
                map.put("username", slurmAccount.getUsername());
                map.put("ip", slurmAccount.getIp());
                SlurmList.add(map);
            }
        }
        return SlurmList;
    }

    /**
     * 获取有Slurm账户的用户
     * @return {用户id: [slurm账户用户名, slurm账户IP], ..., }
     */
    public Map<Long, List> getUserWithSlurmAccount(){
        List<User> users = userDAO.findBySlurmAccountNotNull();
        HashMap<Long, List> userWithSlurmAccount = new HashMap<>();
        for (User user: users){
            List<String> slurmList = new ArrayList<>();
            if (user.getSlurmAccount().getUsername() != null && user.getSlurmAccount().getIp() != null) {
                slurmList.add(user.getSlurmAccount().getUsername());
                slurmList.add(user.getSlurmAccount().getIp());
                userWithSlurmAccount.put(user.getId(),slurmList);
            }
        }
        return userWithSlurmAccount;
    }

}
