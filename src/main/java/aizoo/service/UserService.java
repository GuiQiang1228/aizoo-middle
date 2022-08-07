package aizoo.service;


import aizoo.common.*;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.utils.EnumUtil;
import aizoo.viewObject.object.UserVO;
import aizoo.viewObject.mapper.UserVOEntityMapper;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URISyntaxException;
import java.util.*;

/**
 * UserDetailsService的实现类，用于在程序中引入一个自定义的AuthenticationProvider，实现数据库访问模式的验证
 */
@Service("UserService")
public class UserService implements UserDetailsService {

    @Autowired
    UserDAO userDAO;

    @Autowired
    RoleDAO roleDAO;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    ForkEditionDAO forkEditionDAO;

    @Autowired
    LevelDAO levelDAO;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    UserLevelChangeLogDAO userLevelChangeLogDAO;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
    * @Description: 根据用户提供的username获取用户信息
    * @param username: 用户名
    * @return: aizoo.domain.User：若数据库中存在该用户，则返回查询到用户信息 User(id=xx,create_time=xx,...)
    * @throws: UsernameNotFoundException
    */
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Start load User By Username");
        logger.info("login:{}", username);
        User userBean = userDAO.findByUsername(username);
        if (userBean == null) {
            throw new UsernameNotFoundException("数据库中无此用户！");
        }
        logger.info("loadUserByUsername return:{}",userBean);
        logger.info("End load User By Username");
        return userBean;
    }

    /**
    * @Description: 根据权限信息获取用户列表
     * 使用时，获取所有用户和ADMIN用户来间接查询普通用户
    * @param userRoleType: 用户的权限类别
    * @return: java.util.List<aizoo.viewObject.object.UserVO>：返回查询到的用户列表[UserVO(id=xx,update_time=xx,...),UserVO(...),...]
    */
    //== 该方法有问题：roleType是外部传来的, 正常的role都能查出来, 为什么查完再删一遍
    public List<UserVO> getUserListByRoleOfUser(UserRoleType userRoleType) {
        logger.info("Start get User List By Role Of User");
        logger.info("getUserListByRoleOfUser userRoleType:{}",userRoleType);
        List<User> adminList = userDAO.findByRolesUserRoleType(userRoleType);
        List<User> allUserList = userDAO.findAll();
        List<UserVO> userVOList = new ArrayList<>();
        allUserList.removeAll(adminList);
        for (User user : allUserList) {
            userVOList.add(UserVOEntityMapper.MAPPER.userEntity2UserVO(user));
        }
        logger.info("getUserListByRoleOfUser return:{}",userVOList);
        logger.info("End get User List By Role Of User");
        return userVOList;
    }

    /**
    * @Description: 获取所有用户的信息列表(按照username升序排列)
    * @return: java.util.List<aizoo.viewObject.object.UserVO>:返回所有的用户列表[UserVO(id=xx,update_time=xx,...),UserVO(...),...]
    */
    public List<UserVO> getAllUserByList() {
        logger.info("Start get All User By List");
        //1. 获取数据库中所有的用户信息
        List<User> allUserList = userDAO.findAll(Sort.by(Sort.Direction.ASC, "username"));
        List<UserVO> userVOList = new ArrayList<>();
        //2. 将所有原始数据转为VO数据
        for (User user : allUserList) {
            userVOList.add(UserVOEntityMapper.MAPPER.userEntity2UserVO(user));
        }
        logger.info("getAllUserByList return:{}",userVOList);
        logger.info("End get All User By List");
        return userVOList;
    }

    /**
    * @Description: 使用分页的方式获取用户的信息列表(根据id升序排序)
    * @param pageNum:第pageNum页
    * @param pageSize: 每页的数据数量
    * @return: org.springframework.data.domain.Page<aizoo.viewObject.object.UserVO>
    */
    public Page<UserVO> getAllUserList(Integer pageNum, Integer pageSize) {
        logger.info("Start get All User List");
        logger.info("getAllUserList pageNum:{},pageSize:{}",pageNum,pageSize);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.ASC, "id");
        Page<User> userPage = userDAO.findAll(pageable);
        logger.info("End get All User List");
        return VO2EntityMapper.mapEntityPage2VOPage(UserVOEntityMapper.MAPPER::userEntity2UserVO, userPage);
    }

    /**
    * @Description: 实现用户注册的功能
    * @param username: 用户名
    * @param email: 邮箱
    * @param fe: ForkEdition信息
    * @return: boolean:是否注册成功
    * @throws: ParseException
    */
    public boolean register(String username, String email, ForkEdition fe) {
        logger.info("Start register");
        logger.info("register username:{},email:{},fe:{}",username,email,fe);
        //1. 根据用户名查询是否已经存在用户信息
        User myUser = userDAO.findByUsername(username);
        if (myUser == null) {
            //2. 如果根据用户名没有查到数据，说明可以注册！
            User user = new User(username, email);
            Date levelStartTime = new Date(System.currentTimeMillis());  // new Date()为获取当前系统时间
            user.setLevelStartTime(levelStartTime);
            Calendar rightNow = Calendar.getInstance();
            rightNow.setTime(levelStartTime);
            rightNow.add(Calendar.YEAR, 100);
            Date levelFinishTime = rightNow.getTime();
            user.setLevelFinishTime(levelFinishTime);
            Role role = roleDAO.findByUserRoleType(UserRoleType.USER);
            user.setRoles(Collections.singletonList(role));
            Level level = levelDAO.findByName(LevelType.LEVEL1);
            user.setLevel(level);
            UserStatusType status = UserStatusType.NORMAL_STATUS;
            user.setStatusName(status);
            user.setForkEdition(fe);
            userDAO.save(user);
            logger.info("【用户添加完成】");
            logger.info("register return:true");
            logger.info("End register");
            return true;
        }
        logger.info("register return:false");
        logger.info("End register");
        return false;
    }

    /**
    * @Description: 检查用户名是否可以注册(检查当前用户名是否在数据库中存在)
    * @param username: 当前输入的用户名
    * @return: boolean：用户名是否可以注册 true:用户名数据库中不存在 fasle:用户名已存在
    */
    public boolean checkName(String username) {
        logger.info("Start check Name");
        logger.info("checkName username:{}",username);
        User myUser = userDAO.findByUsername(username);
        if (myUser == null) {
            logger.info("【用户不存在，可以注册】");
            logger.info("End check Name");
            return true;
        }
        logger.info("【用户已存在，不可以注册】");
        logger.info("End check Name");
        return false;
    }

    /**
    * @Description: 检查当前邮箱是否可以注册（检查当前邮箱是否在数据库中存在）
    * @param email: 当前输入的邮箱
    * @return: boolean：true：当前邮箱在数据库中不存在 false:邮箱已经存在
    */
    public boolean checkEmail(String email) {
        logger.info("Start check Email");
        logger.info("checkEmail email:{}",email);
        User myUser = userDAO.findByEmail(email);
        if (myUser == null) {
            logger.info("【该邮箱不存在，可以注册】");
            logger.info("End check Email");
            return true;
        }
        logger.info("【该邮箱已存在，不可以注册】");
        logger.info("End check Email");
        return false;
    }
    
    /**
    * @Description: 更新用户状态
    * @param username: 用户名 
    * @param status: 用户需要更新的状态
    * @return: java.lang.String：更新结果信息
    */
    public String updateUserStatus(String username, String status) {
        logger.info("Start update User Status");
        logger.info("updateUserStatus username:{},status:{}",username,status);
        if (username == null) {
            //1. 若需要更新状态的用户为null，则直接返回
            logger.info("用户名为空");
            logger.info("End update User Status");
            return ("用户名为空");
        } else {
            //2. 查询当前用户是否存在
            User user = userDAO.findByUsername(username);
            if (user == null) {
                logger.info("该用户未注册");
                logger.info("End update User Status");
                return ("该用户未注册");
            } else {
                //3. 若当前用户存在，根据status对用户状态更新
                EnumUtil enumUtil = new EnumUtil();
                if (enumUtil.isInclude(UserStatusType.class, status) == false) {
                    logger.info("没有该状态");
                    logger.info("End update User Status");
                    return ("没有该状态");
                } else {
                    UserStatusType s = UserStatusType.valueOf(status);
                    user.setStatusName(s);
                    userDAO.save(user);
                    logger.info("更改用户状态成功");
                    logger.info("End update User Status");
                    return ("更改用户状态成功");
                }
            }
        }
    }

    /**
    * @Description: 更新用户的等级（包括等级和需要保持的时长）
    * @param username: 需要更新的用户名
    * @param level:    需要更新的level信息
    * @param month:    需要保持的时长
    * @return: java.lang.String：更新结果信息
    */
    public String updateUserLevel(String username, String level, int month) {
        logger.info("Start update User Level");
        logger.info("updateUserLevel username:{},level:{},month:{}",username,level,month);
        if (username == null) {
            // 1. 若当前用户名为null，则直接返回
            logger.info("用户名为空");
            logger.info("End update User Level");
            return ("用户名为空");
        } else {
            // 2. 查询当前用户是否存在
            User user = userDAO.findByUsername(username);
            if (user == null) {
                logger.info("该用户未注册");
                logger.info("End update User Level");
                return ("该用户未注册");
            } else {
                // 3. 当用户存在，则进行等级更新
                EnumUtil enumUtil = new EnumUtil();
                // 3.1 查询需要更新的level是否存在
                if (enumUtil.isInclude(LevelType.class, level) == false) {
                    logger.info("没有该等级");
                    logger.info("End update User Level");
                    return ("没有该等级");
                } else {
                    // 3.2 当需要更新的level存在，则进行更新
                    Level newLevel = levelDAO.findByName(LevelType.valueOf(level));
                    user.setLevel(newLevel);
                    updateStartTimeAndFinishTime(username, month);
                    userDAO.save(user);
                    logger.info("更改用户等级信息成功,等级为:{},时长为:{}个月",level,month);
                    logger.info("End update User Level");
                    return ("更改用户等级信息成功,等级为" + level + ",时长为" + month + "个月");
                }
            }
        }
    }

    /**
    * @Description: 更新等级开始的时间和结束的时间
    * @param username: 需要更新等级的用户名
    * @param month:    当前等级需要保持的时长
    */
    public void updateStartTimeAndFinishTime(String username, int month) {
        logger.info("Start update Start Time And Finish Time");
        logger.info("updateStartTimeAndFinishTime username:{},month:{}",username,month);
        User user = userDAO.findByUsername(username);
        Date levelStartTime = new Date(System.currentTimeMillis());
        // 变更等级的那一刻为startTime
        user.setLevelStartTime(levelStartTime);
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(levelStartTime);
        // month是用户充值的时长，以月为单位，levelFinishTime 应该是 levelStartTime + month
        rightNow.add(Calendar.MONTH, month);
        Date levelFinishTime = rightNow.getTime();
        user.setLevelStartTime(levelStartTime);
        user.setLevelFinishTime(levelFinishTime);
        userDAO.save(user);
        logger.info("End update Start Time And Finish Time");
    }

    /**
    * @Description: 更新等级结束时间(等级续期)
    * @param username: 需要续期的用户名
    * @param month: 需要续期的时长
    * @return: java.lang.String 续期结果信息
    */
    public String updateFinishTime(String username, int month) {
        logger.info("Start update Finish Time");
        logger.info("updateFinishTime username{},month{}",username,month);
        if (username == null) {
            // 1. 当前用户名为null,直接返回null
            logger.info("用户名为空");
            logger.info("End update Finish Time");
            return ("用户名为空");
        } else {
            // 2. 查询当前用户是否存在
            User user = userDAO.findByUsername(username);
            if (user == null) {
                logger.info("该用户未注册");
                logger.info("End update Finish Time");
                return ("该用户未注册");
            } else {
                // 3. 当前用户存在，执行续期操作：在原来结束时间的基础上加month月的时长
                Date previousFinishTime = user.getLevelFinishTime();
                Calendar rightNow = Calendar.getInstance();
                rightNow.setTime(previousFinishTime);
                rightNow.add(Calendar.MONTH, month);
                Date levelFinishTime = rightNow.getTime();
                user.setLevelFinishTime(levelFinishTime);
                userDAO.save(user);
                logger.info("续期成功");
                logger.info("End update Finish Time");
                return ("续期成功");
            }
        }
    }

    /**
    * @Description: 根据用户名获取用户的资源使用情况
    * @param username: 用户名
    * @return: Map<String, Object> 资源使用情况
     * 返回类型是一个map，每个map的key是资源描述，value是具体的资源详情/数值
     * 数据样例参考 docs/UserService.getUserResource.return.md
    * @throws: URISyntaxException
    */
    public Map<String, Object> getUserResource(String username) throws URISyntaxException {
        logger.info("Start get User Resource");
        logger.info("getUserResource username:{}", username);
        // 返回用户个人页面前，更新一下硬盘容量
        resourceUsageService.updateDiskCapacity(username);

        User user = userDAO.findByUsername(username);
        Level level = user.getLevel();
        /* 用户可以获取资源如下:
        最大的 CPU,GPU,memory,DISK,experiment, application总数/在跑数, service总数/在跑数, Disk总数/在跑数
        数据在level表里获取
        */
        int maxCPU = level.getCPU();
        int maxGPU = level.getGPU();
        int maxTotalApp = level.getAppTotalNum();
        int maxTotalExperiment = level.getExperimentTotalNum();
        int maxTotalService = level.getServiceTotalNum();
        int maxTotalMirror = level.getMirrorTotalNum();

        int maxRunningApp = level.getAppMaxRunningNum();
        int maxRunningExperiment = level.getExperimentMaxRunningNum();
        int maxRunningService = level.getServiceMaxRunningNum();
        int maxRunningMirror = level.getMirrorMaxRunningNum();

        Double maxMemory = level.getMemory();
        Double maxDisk = level.getDisk();

        //在resource_Usage表里获取 select sum如果表中没有该user的对应记录, 结果为null
        Double appliedCPU = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.CPU.toString(), username);
        Double appliedGPU = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.GPU.toString(), username);
        Double appliedMemory = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.MEMORY.toString(), username);
        Double appliedDisk = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.DISK.toString(), username);
        Double runningExperiment = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.EXPERIMENT_RUNNING_NUMBER.toString(), username);
        Double runningService = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.SERVICE_RUNNING_NUMBER.toString(), username);
        Double runningApplication = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.APPLICATION_RUNNING_NUMBER.toString(), username);
        Double runningMirror = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.MIRROR_JOB_RUNNING_NUMBER.toString(), username);

        //在对应的job表里获取 select count 如果表中没有该user的对应记录，结果为0
        int existExperiment = experimentJobDAO.countByUserUsername(username);
        int existApplication = applicationDAO.countByUserUsername(username);
        int existService = serviceJobDAO.countByUserUsername(username);
        int existMirror = mirrorJobDAO.countByUserUsername(username);

        Map<String, Object> userResource = new HashMap() {
            {
                put("maxCPU", maxCPU);
                put("maxGPU", maxGPU);
                put("maxDisk", maxDisk);
                put("maxMemory", maxMemory);
                put("maxTotalApp", maxTotalApp);
                put("maxTotalExperiment", maxTotalExperiment);
                put("maxTotalService", maxTotalService);
                put("maxTotalMirror", maxTotalMirror);
                put("maxRunningApp", maxRunningApp);
                put("maxRunningExperiment", maxRunningExperiment);
                put("maxRunningService", maxRunningService);
                put("maxRunningMirror", maxRunningMirror);
                put("appliedCPU", appliedCPU);
                put("appliedGPU", appliedGPU);
                put("appliedMemory", appliedMemory);
                put("appliedDisk", appliedDisk);
                put("runningExperiment", runningExperiment);
                put("runningService", runningService);
                put("runningApplication", runningApplication);
                put("runningMirror", runningMirror);
                put("existExperiment", existExperiment);
                put("existApplication", existApplication);
                put("existService", existService);
                put("existMirror", existMirror);
            }
        };
        logger.info("getUserResource return:{}",userResource);
        logger.info("End get User Resource");
        return userResource;
    }

    /**
    * @Description: 校验用户名的合法性（用户名是否只包含字母和数字，以字母开头且在6-30位）
    * @param username: 待校验的用户名
    * @return: boolean：用户名是否合法
    */
    public boolean isLetterDigitUsername(String username) {
        logger.info("Start is Letter Digit Username");
        logger.info("isLetterDigitUsername:{}",username);
        String regex = "^[a-zA-Z0-9]{6,30}$";
        boolean isLetter = false;
        if (Character.isLetter(username.charAt(0)))
            isLetter = true;
        boolean isRight = isLetter && username.matches(regex);
        if (isRight)
            logger.info("[用户名合法，可以注册]");
        else
            logger.info("[用户名不合法，不可以注册]");
        logger.info("End is Letter Digit Username");
        return isRight;
    }

    /**
    * @Description: 校验密码是否合法（密码是否同时包含且只包含字母和数字，并在8-30位）
    * @param password: 待校验密码
    * @return: boolean 密码是否合法
    */
    public boolean isLetterDigitPassword(String password) {
        logger.info("Start is Letter Digit Password");
        logger.info("isLetterDigitPassword password:{}",password);
        boolean isDigit = false;
        boolean isLetter = false;
        for (int i = 0; i < password.length(); i++) {
            if (Character.isDigit(password.charAt(i))) {
                isDigit = true;
            } else if (Character.isLetter(password.charAt(i))) {
                isLetter = true;
            }
        }
        String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,30}$";
        boolean isRight = isDigit && isLetter && password.matches(regex);
        if (isRight)
            logger.info("[密码合法，可以注册]");
        else
            logger.info("[密码不合法，不可以注册]");
        logger.info("End is Letter Digit Password");
        return isRight;
    }

    /**
    * @Description: 用户是否可以请求升级或者续期
    * @param username: 用户名
    * @return: boolean 如果还有未审核的记录，返回false，不可以请求升级或续期；反之返回true
    */
    public boolean changeLevelAllowed(String username) {
        logger.info("Start change Level Allowed");
        logger.info("changeLevelAllowed username:{}",username);
        List<UserLevelChangeLog> log = userLevelChangeLogDAO.findByUserUsernameAndChanged(username, false);
        if (log.size() > 0) {
            logger.info("changeLevelAllowed return false");
            logger.info("End change Level Allowed");
            return false;
        }
        logger.info("changeLevelAllowed return true");
        logger.info("End change Level Allowed");
        return true;
    }

    /**
    * @Description:       在用户等级更改日志中添加记录，变更等级
    * @param username:    需要变更等级的用户名
    * @param level:       用户申请变更的等级
    * @param month:       用户申请变更时长
    * @param changeType:  申请类别（管理员主动修改、通过支付升级、用户申请三种）
    * @param levelChange: 是否为变更等级的请求
    * @return: java.lang.String 反馈消息
    */
    public String addUserLevelChangeLog(String username, String level, int month, LevelChangeType changeType, boolean levelChange) {
        logger.info("Start add User Level Change Log");
        logger.info("addUserLevelChangeLog username:{},level{},month:{},changeType{},levelChange:{}",username,level,month,changeType,levelChange);
        EnumUtil enumUtil = new EnumUtil();
        // 判断等级是否属于枚举类型
        if (enumUtil.isInclude(LevelType.class, level) == true) {
            User user = userDAO.findByUsername(username);
            UserLevelChangeLog userLevelChangeLog = new UserLevelChangeLog();
            userLevelChangeLog.setUser(user);
            userLevelChangeLog.setChanged(false);//添加log默认初始值为false
            userLevelChangeLog.setChangeTime(null);
            Date date = new Date();
            userLevelChangeLog.setAppliedTime(date);
            userLevelChangeLog.setPreviousLevel(user.getLevel().getName().toString());//这里要确保数据库中的level数据是有的
            userLevelChangeLog.setAppliedLevel(level);
            userLevelChangeLog.setAppliedDuration(month);
            userLevelChangeLog.setChangeType(changeType);
            userLevelChangeLog.setLevelChange(levelChange);
            userLevelChangeLogDAO.save(userLevelChangeLog);
            logger.info("用户提交申请成功");
            logger.info("End add User Level Change Log");
            return "用户提交申请成功";

        } else {
            logger.info("该等级不存在");
            logger.info("End add User Level Change Log");
            return "该等级不存在";
        }
    }

    /**
    * @Description: 在用户等级更改日志中添加记录，续期
    * @param username: 需要续期的用户名
    * @param addTime:  需要续期的时长
    * @param changeType: 申请类别（管理员主动修改、通过支付升级、用户申请三种）
    * @return: java.lang.String 反馈消息
    */
    public String addUserTimeChangeLog(String username, int addTime, LevelChangeType changeType){
        logger.info("Start add User Time Change Log");
        logger.info("addUserTimeChangeLog username:{},addTime:{},changeType:{}",username,addTime,changeType);
        if(addTime >= 0 ){
            User user = userDAO.findByUsername(username);
            UserLevelChangeLog userLevelChangeLog = new UserLevelChangeLog();
            userLevelChangeLog.setUser(user);
            userLevelChangeLog.setChanged(false);//添加log默认初始值为false
            userLevelChangeLog.setChangeTime(null);
            Date date = new Date();
            userLevelChangeLog.setAppliedTime(date);
            userLevelChangeLog.setPreviousLevel(user.getLevel().getName().toString());//这里要确保数据库中的level数据是有的
            userLevelChangeLog.setChangeType(changeType);
            userLevelChangeLogDAO.save(userLevelChangeLog);
            logger.info("addUserTimeChangeLog return:用户提交申请成功");
            logger.info("End add User Time Change Log");
            return "用户提交申请成功";
        }else {
            logger.info("addUserTimeChangeLog return:addTime不符合规范");
            logger.info("End add User Time Change Log");
            return "addTime不符合规范";
        }
    }

    /**
    * @Description: 同意用户变更等级的请求
    * @param username:    申请变更的用户
    * @param changeType:  申请类别（管理员主动修改、通过支付升级、用户申请三种）
    * @param levelChange: 是否为变更等级的请求
    * @return: java.lang.String 反馈消息
    */
    public String agreeUserChangeLevelApply(String username, LevelChangeType changeType,  boolean levelChange) {
        logger.info("Start agree User Change Level Apply");
        logger.info("agreeUserChangeLevelApply username:{},changeType:{},levelChange:{}",username,changeType,levelChange);
        // 1. 查询申请用户未审核的记录
        List<UserLevelChangeLog> log = userLevelChangeLogDAO.findByUserUsernameAndChanged(username, false);//可能会找到多条记录，例如用户申请审核中的，和管理员主动修改的，要避免他们冲突
        String msg = null;
        if (log.size() > 0) {
            // 2. 若存在未审核的记录/管理员主动修改的，逐条进行处理，寻找变更类别为changeType的记录
            for (UserLevelChangeLog oneUserLevelChangeLog : log) {
                if(changeType.equals(oneUserLevelChangeLog.getChangeType())){
                    // 2.1 对于变更类别为changeType的记录进行操作
                    Date date = new Date();
                    oneUserLevelChangeLog.setChangeTime(date);
                    if(levelChange == true) {
                        // 2.2(if) 变更等级操作
                        msg = updateUserLevel(username, oneUserLevelChangeLog.getAppliedLevel(), oneUserLevelChangeLog.getAppliedDuration());//更改用户等级和时长
                    } else {
                        // 2.2(else) 续期操作
                        msg = updateFinishTime(username, oneUserLevelChangeLog.getAppliedDuration());
                    }
                    // 2.3 设置当前记录的状态为：已审核
                    oneUserLevelChangeLog.setChanged(true);
                    userLevelChangeLogDAO.save(oneUserLevelChangeLog);
                    break;
                }
            }
        } else {
            msg = "该用户没有等级申请审核中";
        }
        logger.info("agreeUserChangeLevelApply return:{}",msg);
        logger.info("End agree User Change Level Apply");
        return msg;
    }

    /**
    * @Description: 如果username在数据库里不存在，则加到数据库中，这个方法必须串行,且开启事务
    * @param username: 用户名
    * @param email:    邮箱
    * @return: void
    */
    @Transactional
    public synchronized void addUserSync(String username, String email){
        logger.info("Start add User Sync");
        logger.info("addUserSync username:{},email{}",username,email);
        User user=userDAO.findByUsername(username);
        if (user!=null) {
            logger.info("user!=null, End add User Sync");
            return;
        }
        try {
            ForkEdition fe = new ForkEdition(0);
            this.register(username, email, fe);
            logger.info("Username: {} is added", username);
        }
        catch (Exception e){
            logger.error("Username: {} is already registered, 错误: ", username, e.getMessage());
        }
        logger.info("End add User Sync");
    }
}