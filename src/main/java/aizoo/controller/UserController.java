package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.common.LevelChangeType;
import aizoo.domain.Level;
import aizoo.domain.User;
import aizoo.domain.UserLevelChangeLog;
import aizoo.repository.LevelDAO;
import aizoo.repository.UserDAO;
import aizoo.repository.UserLevelChangeLogDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.UserService;
import aizoo.viewObject.mapper.UserVOEntityMapper;
import aizoo.viewObject.object.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 普通用户
 */

@Controller
@BaseResponse
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    UserDAO userDAO;

    @Autowired
    UserLevelChangeLogDAO userLevelChangeLogDAO;

    @Autowired
    LevelDAO levelDAO;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
    * @Description: 获取当前登录用户的VO信息
    * @param principal: 用户登录信息
    * @return: aizoo.viewObject.object.UserVO {id,updateTime,username,roles,level,levelFinishTime}
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/getCurrentUser", method = RequestMethod.GET)
    @WebLog(description = "获取当前登录用户")
    @ResponseBody
    public UserVO currentUserName(Principal principal) {
        User user = userDAO.findByUsername(principal.getName());
        return UserVOEntityMapper.MAPPER.userEntity2UserVO(user);
    }

    /**
    * @Description: 检查用户名是否合法
    * @param username: 待检查的username
    * @return: aizoo.response.ResponseResult：包括code，message和true/false
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/register/checkUsername", method = RequestMethod.GET)
    @WebLog(description = "检查用户名是否存在以及是否合法")
    @ResponseBody
    public ResponseResult checkName(String username) {
        if (username != null) {
            boolean flag = userService.checkName(username);
            if (flag && userService.isLetterDigitUsername(username)) {
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), true);  //用户名不存在且用户名合法，可以注册
            }
            if(flag)
                return new ResponseResult(ResponseCode.USERNAME_RULE_ERROR.getCode(), ResponseCode.USERNAME_RULE_ERROR.getMsg(), false);
        }
        return new ResponseResult(ResponseCode.USERNAME_CHECK_ERROR.getCode(), ResponseCode.USERNAME_CHECK_ERROR.getMsg(), false); //用户名已存在或者不合法，不能注册
    }

    /**
    * @Description: 检查邮箱是否已经被注册
    * @param email: 待检查的邮箱
    * @return: aizoo.response.ResponseResult：包括code，message和true/false
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/register/checkEmail", method = RequestMethod.GET)
    @WebLog(description = "检查邮箱是否已经被注册")
    @ResponseBody
    public ResponseResult checkEmail(String email) {
        if (email != null) {
            boolean flag = userService.checkEmail(email);
            if (flag) {
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), true);
            }
        }
        return new ResponseResult(ResponseCode.EMAIL_CHECK_ERROR.getCode(), ResponseCode.EMAIL_CHECK_ERROR.getMsg(), false);
    }

    /**
    * @Description: 获取用户资源最大数量和已使用数量
    * @param principal: 用户登录信息
    * @return: java.util.Map<java.lang.String,java.lang.Object>：包含maxCPU，maxGPU等信息
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/getUserResource", method = RequestMethod.GET)
    @WebLog(description = "获取用户资源最大数量和已使用数量")
    @ResponseBody
    public Map<String, Object> getUserResource(Principal principal) throws URISyntaxException {
        return userService.getUserResource(principal.getName());
    }

    /**
    * @Description: 获取当前登录用户的等级和到期时间
    * @param principal: 用户登录信息
    * @return: java.util.Map<java.lang.String,java.lang.String>: 以level和levelFinishTime为key的map
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/getUserLevelAndTime", method = RequestMethod.GET)
    @WebLog(description = "获取当前登录用户的等级和到期时间")
    @ResponseBody
    public Map<String, String> getUserLevelAndTime(Principal principal) {
        logger.info("获取当前登录用户的等级和到期时间");
        String username = principal.getName();
        User user = userDAO.findByUsername(username);
        String level = user.getLevel().getName().toString();
        String levelFinishTime = user.getLevelFinishTime().toString();
        Map<String, String> userLevelMessage = new HashMap() {
            {
                put("level", level);
                put("levelFinishTime", levelFinishTime);

            }
        };
        return userLevelMessage;
    }

    /**
    * @Description: 用户是否可以请求升级或者续期
    * @param principal: 用户登录信息
    * @return: boolean 是否可以请求升级/续期
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/getLevelChangeStatus", method = RequestMethod.GET)
    @WebLog(description = "用户是否可以请求升级或者续期")
    @ResponseBody
    public boolean changeLevelAllowed(Principal principal) {
        return userService.changeLevelAllowed(principal.getName());
    }

    /**
    * @Description: 用户请求升级
    * @param level: 用户申请变更的等级
    * @param month: 用户申请变更时长
    * @param principal: 用户的登录信息
    * @return: aizoo.response.ResponseResult:包含操作反馈code，message
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/applyChangeLevel", method = RequestMethod.POST)
    @WebLog(description = "用户请求升级")
    @ResponseBody
    public ResponseResult addLevelChangeLog(@MultiRequestBody String level, @MultiRequestBody int month, Principal principal) {
        //判断level是否属于枚举类
        if (userService.changeLevelAllowed(principal.getName())) {
            boolean levelChange = true;
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), userService.addUserLevelChangeLog(principal.getName(), level, month, LevelChangeType.USER_APPLY, levelChange), null);
        } else {
            return new ResponseResult(ResponseCode.LEVEL_CHANGE_ERROR.getCode(), ResponseCode.LEVEL_CHANGE_ERROR.getMsg(), null);
        }
    }

    /**
    * @Description: 用户请求续期
    * @param addTime:   请求续期的时长
    * @param principal: 用户登录信息
    * @return: aizoo.response.ResponseResult:包含操作反馈code，message
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/applyChangeLevelFinishTime", method = RequestMethod.POST)
    @WebLog(description = "用户请求续期")
    @ResponseBody
    public ResponseResult addLevelChangeLog(@MultiRequestBody int addTime, Principal principal){
        if(userService.changeLevelAllowed(principal.getName())){
            User user = userDAO.findByUsername(principal.getName());
            String level = user.getLevel().getName().toString();
            boolean levelChange = false;
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), userService.addUserLevelChangeLog(principal.getName(), level, addTime, LevelChangeType.USER_APPLY, levelChange), null);
        }else {
            return new ResponseResult(ResponseCode.LEVEL_CHANGE_ERROR.getCode(), ResponseCode.LEVEL_CHANGE_ERROR.getMsg(), null);
        }

    }

    /**
    * @Description: 获取所有的等级信息
    * @return: java.util.List<aizoo.domain.Level>:包含level信息的列表
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/getAllLevelInformation", method = RequestMethod.GET)
    @WebLog(description = "获取所有的等级信息")
    @ResponseBody
    public List<Level> getAllLevelInformation(){
        List<Level> level = levelDAO.findAll();
        return level;
    }

    /**
    * @Description: 获取当前用户所有的等级变更申请
    * @param principal: 用户的登录信息
    * @return: java.util.List<aizoo.domain.UserLevelChangeLog>:包含等级变更申请信息的列表
    * @throws:
    */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/getMyLevelApply", method = RequestMethod.GET)
    @WebLog(description = "获取当前用户所有的等级变更申请")
    @ResponseBody
    public List<UserLevelChangeLog> getMyLevelApply (Principal principal){
       List<UserLevelChangeLog> log = userLevelChangeLogDAO.findByUserUsernameAndChangeType(principal.getName(), LevelChangeType.USER_APPLY);
       logger.info("获取当前用户所有的等级变更申请");
       return log;
    }
}
