package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.common.*;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.*;
import java.net.URISyntaxException;
import aizoo.viewObject.mapper.*;
import aizoo.viewObject.object.*;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户
 */
@BaseResponse
@Controller
public class SecurityController {
    @Autowired
    UserDAO userDAO;

    @Autowired
    UserService userService;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    UserLevelChangeLogDAO userLevelChangeLogDAO;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    private ResourceUsageService resourceUsageService;

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private DatasourceDAO datasourceDAO;

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    /**
     * 使用getUserListByRoleOfUser方法
     * 分别获取所有用户和ADMIN用户来间接查询普通用户
     * @return 返回一个普通用户的列表
     * @throws
     */
    //== getUserListByRoleOfUser方法有问题
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/getUserListByRoleOfUser", method = RequestMethod.GET)
    @WebLog(description = "管理员获取当前所有普通用户")
    @ResponseBody
    public List<UserVO> getUserListByRoleOfUser() {return userService.getUserListByRoleOfUser(UserRoleType.ADMIN);}


    /**
     * 该方法使用getAllUserList方法
     * 获取所有用户的信息
     * @param pageNum 页数
     * @param pageSize 页宽
     * @return 返回全部用户的信息
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/getAllUserList", method = RequestMethod.GET)
    @WebLog(description = "获取全部用户的信息")
    @ResponseBody
    public Page<UserVO> getAllUserList(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return userService.getAllUserList(pageNum, pageSize);
    }

    /**
     * 获取所有用户
     * @return 返回一个包含所有用户的列表
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/getAllUserByList", method = RequestMethod.GET)
    @WebLog(description = "管理员获取当前所有用户")
    @ResponseBody
    public List<UserVO> getUserList() {
        return userService.getAllUserByList();
    }

    /**
     * 变更用户的状态
     * @param username 用户名
     * @param userStatus 变更后的状态
     * @return 返回一个信息体(成功/失败)
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/updateUserStatus", method = RequestMethod.POST)
    @WebLog(description = "更改用户的状态")
    @ResponseBody
    public ResponseResult updateUserStatus(@MultiRequestBody(value = "username") String username, @MultiRequestBody(value = "status") String userStatus) {
        String msg = userService.updateUserStatus(username, userStatus);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 变更用户的等级 更新等级信息变化日志
     * @param month     时长
     * @param username  用户名
     * @param userLevel 用户等级
     * @return 返回一个信息体(成功/失败)
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/updateUserLevel", method = RequestMethod.POST)
    @WebLog(description = "管理员更改用户等级和时长，并且更新等级信息变化日志")
    @ResponseBody
    public ResponseResult updateUserLevel(@MultiRequestBody(value = "username") String username,
                                          @MultiRequestBody(value = "level") String userLevel,
                                          @MultiRequestBody(value = "levelTime") int month) {
        boolean levelChanged = true;
        //通过接口和参数信息添加日志 包含用户名、用户级别、月份、级别变动类型
        userService.addUserLevelChangeLog(username, userLevel, month, LevelChangeType.SUPER_CHANGE, levelChanged);
        String msg = userService.agreeUserChangeLevelApply(username, LevelChangeType.SUPER_CHANGE, true);
        logger.info("管理员更新用户等级和时长");
        //这个接口不需要定义失败的code，因为传参不会异常
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 变更用户的等级时长 更新等级信息变化时长日志信息
     * @param month     时长
     * @param username  用户名
     * @return 返回一个响应结果以状态码和日志信息表示
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "admin/updateLevelTime", method = RequestMethod.POST)
    @WebLog(description = "管理员变更某等级时长,并且更新等级信息变化日志")
    @ResponseBody
    public ResponseResult updateLevelTime(@MultiRequestBody(value = "username") String username,
                                          @MultiRequestBody(value = "addTime") int month) {
        User user = userDAO.findByUsername(username);
        boolean levelChanged = false;
        //变更等级和变更时长不能用同一个接口来添加日志，因为无法区分是addtime还是time
        userService.addUserLevelChangeLog(username, user.getLevel().getName().toString(), month, LevelChangeType.SUPER_CHANGE, levelChanged);
        String msg = userService.agreeUserChangeLevelApply(username, LevelChangeType.SUPER_CHANGE, levelChanged);

        return new ResponseResult(ResponseCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 同意用户申请变更等级信息 更新等级信息变化日志信息
     * @param username  用户名
     * @return 返回一个响应结果以状态码和日志信息表示
     * @throws
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "admin/agreeUserChangeLevelApply", method = RequestMethod.POST)
    @WebLog(description = "管理员同意用户申请变更等级信息(升级或者续期),并且更新等级信息变化日志")
    @ResponseBody
    public ResponseResult agreeUserChangeLevelApply(@MultiRequestBody String username) {
        // 列出所有申请等级变更的日志
        List<UserLevelChangeLog> changedLog = userLevelChangeLogDAO.findByUserUsernameAndChanged(username, false);
        String msg = null;
        for (UserLevelChangeLog changedType : changedLog) {
            // 只有用户申请的变更（USER_APPLY）才需要被处理
            if ((changedType.getChangeType().toString()).equals(LevelChangeType.USER_APPLY.toString())) {
                // 判断是否申请变更
                boolean levelChanged = changedType.isLevelChange();
                msg = userService.agreeUserChangeLevelApply(username, LevelChangeType.USER_APPLY, levelChanged);
                break;
            }
        }
        if (msg == null) {
            msg = "该用户没有等级信息变更申请";
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 获取所有用户的升级或者续期请求 更新等级信息变化日志信息
     * 先获取所有用户状态的日志信息 然后从中判断是否改变状态
     * @param pageNum 页数
     * @param pageSize 页宽
     * @return 返回一个信息体(成功/失败)
     * @throws JsonProcessingException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "admin/getALlUserLevelChangeLog", method = RequestMethod.GET)
    @WebLog(description = "获取所有用户的升级或者续期请求")
    @ResponseBody
    public ResponseResult getAllUserLevelChangeLog(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws JsonProcessingException {
        // 通过Sort.Order对象的List集合创建Sort对象，适合所有情况，比较容易设置排序方式:升序
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.ASC, "changed"));
        orders.add(new Sort.Order(Sort.Direction.ASC, "appliedTime"));
        // levelChange为true：升级请求；为false：续期请求
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(orders));
        Page<UserLevelChangeLog> levelChangeLogPage = userLevelChangeLogDAO.findAll(pageable);
        // 获取所有有关用户等级改变的日志信息
        List<UserLevelChangeLog> userLevelChangeLogs = levelChangeLogPage.getContent();
        // 记录用户请求的数量
        int requestNumber = 0;
        for (int i = 0; i < userLevelChangeLogs.size(); i++)
            if (userLevelChangeLogs.get(i).isChanged() == false)
                requestNumber++;
        logger.info("获取所有用户的升级或者续期请求, 未处理的升级或续期请求数量为: {}", requestNumber);
        ObjectMapper objectMapper = new ObjectMapper();
        String pageString = objectMapper.writeValueAsString(levelChangeLogPage);
        Map<String, Object> pageMap = objectMapper.readValue(pageString, new TypeReference<Map<String, Object>>() {
        });
        pageMap.put("requestNumber", requestNumber);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), pageMap);
    }

    /**
     * 万能分页用户搜索
     * 根据参数和相应规则通过数据库接口查询用户信息
     * @param username 用户名
     * @param level 用户等级
     * @param levelExpire 等级是否过期
     * @param role 组件角色
     * @param pageNum 页数
     * @param pageSize 页宽
     * @return 返回搜索到的用户信息页面
     * @throws
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/user/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页用户搜索")
    public Page<UserVO> searchPage(@RequestParam(value = "username", required = false, defaultValue = "") String username,
                                   @RequestParam(value = "level", required = false, defaultValue = "") String level,
                                   @RequestParam(value = "role", required = false, defaultValue = "") String role,
                                   @RequestParam(value = "levelExpire", required = false, defaultValue = "") String levelExpire,
                                   @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        if (role.length() > 5)
            role = role.substring(5);
        // 根据参数和相应规则通过数据库接口查询用户信息
        Page<User> appsPage = userDAO.searchUser(username, level, role, levelExpire, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(UserVOEntityMapper.MAPPER::userEntity2UserVO, appsPage);
    }

    /**
     * 根据id删除管理员上传管理页面中用户拥有的组件
     * @param id 组件id
     * @param principal 封装对所有安全主体通用的帐户数据和操作的对象
     * @return 返回一个信息体(成功/失败)
     * @throws Exception URISyntaxException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/resource/deleteComponent", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除管理员上传管理页面用户拥有的组件")
    public ResponseResult adminDeleteComponent(@MultiRequestBody("id") long id, Principal principal) throws Exception{
        // 根据组件id删除组件
        componentService.deleteUploadComponent(id);
        // 重新更新该用户所拥有的的组件
        resourceUsageService.updateDiskCapacity(principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 删除管理员上传管理页面用户拥有的数据资源
     * @param id 资源id
     * @param principal
     * @return 返回一个信息体(成功/失败)
     * @throws URISyntaxException Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/resource/deleteDataResource", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除管理员上传管理页面用户拥有的数据资源")
    public ResponseResult adminDeleteData(@MultiRequestBody("id") long id, Principal principal) throws Exception{
        // 根据数据资源id删除资源
        datasourceService.deleteDatasource(id);
        // 重新更新该用户所拥有的的资源
        resourceUsageService.updateDiskCapacity(principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 通过参数和数据库接口实现万能分页experiment搜索
     * @param jobName 任务名
     * @param desc 描述
     * @param owner 所有者
     * @param jobStatus 任务状态
     * @param graphName 图的名称
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的experiment页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/job/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页experiment搜索")
    public Page<ExperimentJobVO> jobSearchPage(@RequestParam(value = "jobName", required = false, defaultValue = "") String jobName,
                                               @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                               @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                               @RequestParam(value = "jobStatus", required = false, defaultValue = "") String jobStatus,
                                               @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                               @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                               @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                               @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询experiment信息
        Page<ExperimentJob> jobsPage = experimentJobDAO.adminSearchJob(jobName, desc, jobStatus, graphName, startUpdateTime, endUpdateTime, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ExperimentJobVOEntityMapper.MAPPER::job2JobVO, jobsPage);
    }

    /**
     * 通过参数和数据库接口实现万能分页service搜索
     * @param serviceName 服务名
     * @param desc 描述
     * @param serviceStatus 服务状态
     * @param graphName 图的名称
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param owner 所有者
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的service页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/service/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页Service搜索")
    public Page<ServiceJobVO> adminServiceSearchPage(@RequestParam(value = "serviceName", required = false, defaultValue = "") String serviceName,
                                                     @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                                     @RequestParam(value = "serviceStatus", required = false, defaultValue = "") String serviceStatus,
                                                     @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                                     @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                     @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                     @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                                     @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询service信息
        Page<ServiceJob> servicesPage = serviceJobDAO.adminSearchServiceJob(serviceName, desc, serviceStatus, graphName, startUpdateTime, endUpdateTime, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ServiceJobVOEntityMapper.MAPPER::serviceJob2ServiceJobVO, servicesPage);
    }

    /**
     * 通过参数和数据库接口实现万能分页App搜索
     * @param appName app名称
     * @param desc 描述
     * @param graphName 图的名称
     * @param applicationStatus app状态 
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param owner 所有者
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的application页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/application/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页App搜索")
    public Page<ApplicationVO> searchPage(@RequestParam(value = "appName", required = false, defaultValue = "") String appName,
                                          @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                          @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                          @RequestParam(value = "applicationStatus", required = false, defaultValue = "") String applicationStatus,
                                          @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                          @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                          @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                          @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询application信息
        Page<Application> appsPage = applicationDAO.adminSearchApplication(appName, desc, graphName, applicationStatus, startUpdateTime, endUpdateTime, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ApplicationVOEntityMapper.MAPPER::application2ApplicationVO, appsPage);
    }

    /**
     * 通过参数和数据库接口实现万能分页namespace搜索
     * @param namespace 命名空间
     * @param type 类型
     * @param privacy 权限：public/protected/private
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param owner 所有者
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的experiment页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/resource/namespace/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页namespace搜索")
    public Page<NamespaceVO> searchNamespacePage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                 @RequestParam(value = "type", required = false, defaultValue = "") String type,
                                                 @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                 @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                 @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                 @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                                 @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询namespace信息
        Page<Namespace> namespacePage = namespaceDAO.adminSearchNamespace(namespace, type, privacy, startUpdateTime, endUpdateTime, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(NamespaceVOEntityMapper.MAPPER::namespace2NamespaceVO, namespacePage);
    }

    /**
     * 通过参数和数据库接口实现万能分页组件搜索
     * @param namespace 命名空间
     * @param type 类型
     * @param privacy 权限：public/protected/private
     * @param desc 描述
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param name 组件名称
     * @param owner 所有者
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的组件页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/resource/component/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页component搜索")
    public Page<ComponentVO> searchComponentPage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                 @RequestParam(value = "type", required = false, defaultValue = "") String type,
                                                 @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                 @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                                 @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                 @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                 @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                 @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                                 @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询组件信息
        Page<Component> namespacePage = componentDAO.adminSearchComponent(namespace, type, privacy, desc, startUpdateTime, endUpdateTime, name, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ComponentVOEntityMapper.MAPPER::component2ComponentVO, namespacePage);
    }

    /**
     * 通过参数和数据库接口实现万能分页datasource搜索
     * @param namespace 命名空间
     * @param privacy 权限：public/protected/private
     * @param desc 描述
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param name datasource名称
     * @param owner 所有者
     * @param pageNum 页数
     * @param pageSize 页长
     * @return 返回搜索到的datasource页面
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/resource/datasource/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页datasource搜索")
    public Page<DatasourceVO> searchDatasourcePage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                   @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                   @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                                   @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                   @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                   @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                   @RequestParam(value = "owner", required = false, defaultValue = "") String owner,
                                                   @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        // 根据参数获取指定页面大小和页数的page
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 根据参数和相应规则通过数据库接口查询datasource信息
        Page<Datasource> namespacePage = datasourceDAO.adminSearchDatasource(namespace, privacy, desc, startUpdateTime, endUpdateTime, name, owner, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(DatasourceVOEntityMapper.MAPPER::datasource2DatasourceVO, namespacePage);
    }

    /**
     * 通过jobid删除指定实验
     * @param jobId 作业号
     * @return 返回是否删除成功
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/job/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除指定实验")
    public void removeJob(@MultiRequestBody long jobId, Principal principal) throws Exception {
        experimentService.removeExperimentJobById(jobId);
    }

    /**
     * 通过appid删除指定app
     * @param appId
     * @return 返回是否删除成功
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/application/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除指定app")
    public boolean removeJob(@MultiRequestBody("applicationJobId") long appId) throws Exception {
        return applicationService.removeAppById(appId);
    }

    /**
     * 通过serviceJobId删除指定serviceJob
     * @param serviceJobId 服务任务Id
     * @return 返回是否删除成功
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/service/delete", method = RequestMethod.POST)
    @WebLog(description = "serviceJob删除")
    public boolean serviceDelete(@MultiRequestBody long serviceJobId, Principal principal) throws Exception {
        return serviceService.deleteServiceJob(serviceJobId, principal.getName());
    }

    /**
     * 通过JobId终止指定的实验
     * 通过slurm获取job的状态 判断与数据库状态的一致性
     * @param experimentJobId
     * @return 返回是否终止成功
     * @throws JsonProcessingException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/job/stop", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "终止指定实验")
    public boolean stopJob(@MultiRequestBody long experimentJobId) throws Exception {
        boolean result = false;
        // 如果与数据库中的不一致则返回false，并更新数据库；一致则终止任务并查询slurm更新数据库并返回true
        try {
            result = experimentService.slurmStopExperimentJob(experimentJobId);
            logger.info("终止experimentJob成功, experimentJobId = {}", experimentJobId);
        } catch (JsonProcessingException e) {
            logger.error("终止experimentJob失败, experimentJobId = {}, 错误: ", experimentJobId, e.getMessage());
        }
        return result;
    }

    /**
     * 通过jobId终止指定的Job
     * 通过slurm获取job的状态 判断与数据库状态的一致性
     * @param serviceJobId
     * @return 返回是否终止成功
     * @throws JsonProcessingException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/service/stop", method = RequestMethod.POST)
    @WebLog(description = "serviceJob停止")
    public boolean serviceStop(@MultiRequestBody long serviceJobId) throws Exception {
        //如果与数据库中的不一致则返回false，并更新数据库；一致则终止任务并查询slurm更新数据库并返回true
        boolean result = false;
        try {
            result = serviceService.slurmStopServiceJob(serviceJobId);
            logger.info("终止serviceJob成功, serviceJobId = {}", serviceJobId);
        } catch (JsonProcessingException e) {
            logger.error("终止serviceJob失败, serviceJobId = {}, 错误:", serviceJobId, e.getMessage());
        }
        return result;
    }

    /**
     * 通过jobId终止指定的app
     * 通过slurm获取job的状态 判断与数据库状态的一致性
     * @param applicationJobId
     * @return 返回是否终止成功
     * @throws JsonProcessingException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/admin/application/stop", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "停止指定app")
    public boolean stopAppJob(@MultiRequestBody("applicationJobId") long applicationJobId) throws Exception {
        boolean result = false;
        //如果与数据库中的不一致则返回false，并更新数据库；一致则终止任务并查询slurm更新数据库并返回true
        try {
            result = applicationService.slurmStopApplication(applicationJobId);
            logger.info("终止applicationJob成功, applicationJodId = {}", applicationJobId);
        } catch (JsonProcessingException e) {
            logger.error("终止applicationJob失败, applicationJodId = {}, 错误:", applicationJobId, e.getMessage());
        }
        return result;
    }
}
