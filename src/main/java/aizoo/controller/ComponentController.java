package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.common.ComponentType;
import aizoo.domain.Graph;
import aizoo.domain.Service;
import aizoo.domain.User;
import aizoo.repository.GraphDAO;
import aizoo.repository.ServiceDAO;
import aizoo.repository.UserDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ComponentService;
import aizoo.utils.ComponentUtil;
import aizoo.utils.FrameworkUtil;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;
import aizoo.repository.ComponentDAO;
import aizoo.domain.Component;

import javax.persistence.EntityNotFoundException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.*;

@BaseResponse
@RestController
public class ComponentController {

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ServiceDAO serviceDAO;

    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ComponentService componentService;


    @Value("${file.path}")
    String file_path;

    private static final Logger logger = LoggerFactory.getLogger(ComponentController.class);


    /**
     * 获取画图页面的目录
     * @param type 组件类别
     * @return 相应type的目录
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/catalogue", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取画图页面的目录")
    public Map<String, Map<String, String>> getCatalogue(String type) {
        return ComponentUtil.getCatalogue(type);
    }


    /**
     * 获取指定类别的组件
     * @param privacy public/private，为 public时，传回公共数据，private传回用户数据
     * @param type 类别
     * @param principal 包含用户的标识和用户的所属角色的对象
     * @return 获取道德指定类别的ComponentVO实体列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/component", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取指定类别的组件")
    public List<ComponentVO> getComponentByType(@RequestParam String privacy, @RequestParam String type,
                                                Principal principal) {
        return ListEntity2ListVO.component2ComponentVO(componentService.getComponentByType(principal.getName(), privacy, type));
    }


    /**
     *获取指定类别并且指定名称的组件
     * @param componentName 组件名称
     * @param componentType 组件类别
     * @param principal 包含用户的标识和用户的所属角色的对象
     * @return 获取到的指定类别指定名称的ComponentVO实体
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getComponentByNameAndType", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取指定类别指定名称的组件")
    public ComponentVO getComponentByNameAndType(@RequestParam String componentName, @RequestParam String componentType,
                                                 Principal principal) {
        Component component = componentDAO.findByNameAndComponentType(componentName, ComponentType.valueOf(componentType));
        ComponentVO componentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(component);
        return componentVO;
    }

    /**
     * 根据关键字搜索组件，返回公共和个人组件的列表
     * @param keyword 关键字
     * @param type 类型
     * @param principal 包含用户信息的对象
     * @return 获得模糊搜索的公开组件和不公开组件的列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/searchResource", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "根据关键字搜索组件，返回公共+个人")
    public HashMap<String, List<Object>> getAllComponentByKeyword(@RequestParam String keyword, @RequestParam String type, Principal principal) {
        return componentService.getAllComponentByKeyword(principal.getName(), keyword, type);
    }

    /**
     * 根据关键字搜索指定类别的组件
     * @param privacy public/private，为 public时，传回公共数据，private传回用户数据
     * @param type 类别
     * @param keyword 关键字
     * @param principal 包含用户信息的对象
     * @return 获取到的组件实体列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/searchComponent", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "根据关键字搜索指定类别的组件")
    public List<ComponentVO> getDataByKeyword(@RequestParam String privacy, @RequestParam String type,
                                              @RequestParam String keyword, Principal principal) {
        return ListEntity2ListVO.component2ComponentVO(componentService.getComponentByKeyword(privacy, principal.getName(), type, keyword));
    }


    /**
     * 根据service的id和类别判断该组件是否允许fork
     * @param sourceId service的id
     * @param type 类别
     * @return true or false
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/design/allowedFork", method = RequestMethod.GET)
    @WebLog(description = "判断该组件是否允许fork")
    public ResponseResult allowedFork(@RequestParam("sourceId") Long sourceId,
                                      @RequestParam("type") String type) {
        //判断能否进行fork，可以则返回true，否则返回false
        if (!componentService.allowedFork(sourceId, type)) {
            return new ResponseResult(ResponseCode.FORK_NOT_EXIST.getCode(), ResponseCode.FORK_NOT_EXIST.getMsg(), false);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), "可以进行fork", true);
    }

    /**
     * 获取对type的fork信息
     * @param sourceId service的id
     * @param description
     * @param type 类别
     * @param principal 包含用户信息的对象
     * @return 结果信息，包括成功或失败信息以及相应type的实体
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/design/allTypeFork", method = RequestMethod.POST)
    @WebLog(description = "所有类型的fork汇总接口")
    public ResponseResult allTypeFork(@MultiRequestBody("sourceId") long sourceId,
                                      @MultiRequestBody("description") String description,
                                      @MultiRequestBody("type") String type,
                                      Principal principal) {
        HashMap<Long, Long> forkedComponent = new HashMap<>();
        HashMap<Long, Long> forkedService = new HashMap<>();
        HashMap<Long, Long> forkedDatasource = new HashMap<>();
        //获取目标用户对象
        User targetUser = userDAO.findByUsername(principal.getName());
        //如果type为COMPONENT，则获取目标组件，如果目标组件的Privacy为private，则返回私有资源无法fork
        if (type.equals("COMPONENT")) {
            //获取原组件实体
            Component sourceComponent = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            //如果组件的Privacy为"private，则组件fork失败，返回结果实体，失败信息
            if (sourceComponent.getPrivacy().equals("private")) {
                return new ResponseResult(ResponseCode.PRIVACY_ERROR.getCode(), ResponseCode.PRIVACY_ERROR.getMsg(), null);
            }
            if (!sourceComponent.isComposed()) {
                try {
                    //对原子组件进行fork
                    Component component1 = componentService.newFork(targetUser, sourceId, "private", description, true, forkedComponent, forkedService, forkedDatasource);
                    //获取Component实体
                    Component component = componentDAO.findById(component1.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(component1.getId())));
                    //将Component实体转换为ComponentVO实体
                    ComponentVO componentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(component);
                    //原子组件fork成功返回结果实体，成功信息以及组件实体
                    return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", componentVO);
                } catch (Exception exception) {
                    //原子组件fork失败，返回结果实体，失败信息
                    logger.info("组件fork失败!!e={}", exception.getMessage());
                    exception.printStackTrace();
                    return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "组件fork失败", null);
                }
            } else {
                try {
                    //对复合组件进行fork
                    Component component1 = componentService.newFork(targetUser, sourceId, "private", description, false, forkedComponent, forkedService, forkedDatasource);
                    //获取Component实体
                    Component component = componentDAO.findById(component1.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(component1.getId())));
                    //将Component实体转换为ComponentVO实体
                    ComponentVO componentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(component);
                    //复合组件fork成功返回结果实体，成功信息以及组件实体
                    return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", componentVO);
                } catch (Exception exception) {
                    //复合组件fork失败，返回结果实体，失败信息
                    logger.info("组件fork失败!!e={}", exception.getMessage());
                    exception.printStackTrace();
                    return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "组件fork失败", null);
                }
            }
        } else if (type.equals("SERVICE")) {
            //如果type为SERVICE
            try {
                //获取原Service
                Service sourceService=serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
                //如果Service的Privacy为"private，则Service fork失败，返回结果实体，失败信息
                if (sourceService.getPrivacy().equals("private")) {
                    return new ResponseResult(ResponseCode.PRIVACY_ERROR.getCode(), ResponseCode.PRIVACY_ERROR.getMsg(), null);
                }
                //Service fork成功，返回结果实体，成功信息和Service实体
                Service service = componentService.forkService(sourceId, principal.getName(), forkedComponent, forkedService, forkedDatasource);
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", service);
            } catch (Exception exception) {
                //Service fork失败，返回结果实体，失败信息
                logger.info("服务fork失败!!e={}", exception.getMessage());
                exception.printStackTrace();
                return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "服务fork失败", null);
            }
        } else if (type.equals("EXPERIMENT")) {
            //若type为EXPERIMENT
            try {
                //获取目标实验
                Graph targetExperiment = componentService.forkExperimentGraph(sourceId, principal.getName(), forkedComponent, forkedService, forkedDatasource);
                //将目标实验图转化为GraphVO实体
                GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(targetExperiment);
                //实验图fork成功，返回结果包括成功信息和GraphVO实体
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", graphVO);
            } catch (Exception exception) {
                //实验图fork失败，返回结果包括失败信息
                logger.info("实验图fork失败!!e={}", exception.getMessage());
                exception.printStackTrace();
                return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "实验图fork失败", null);
            }
        } else if (type.equals("APPLICATION")) {
            //若type为APPLICATION
            try {
                //获取新的目标应用图
                Graph targetApplication = componentService.forkApplication(sourceId, principal.getName(), forkedComponent, forkedService, forkedDatasource);
                //将目标应用图转化为GraphVO实体
                GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(targetApplication);
                //应用图fork成功，返回结果包括成功信息和GraphVO实体
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", graphVO);
            } catch (Exception exception) {
                //应用图fork失败，返回结果包括失败信息
                logger.info("应用图fork失败!!e={}", exception.getMessage());
                exception.printStackTrace();
                return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "应用fork失败", null);
            }
        } else {
            //该类型无法fork
            return new ResponseResult(ResponseCode.FORK_COMPONENT_ERROR.getCode(), "该类型无法fork", null);
        }
    }


    /**
     * 判断要跳转的该图是否存在
     * @param graphId
     * @return
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/graphIsExisted", method = RequestMethod.GET)
    @WebLog(description = "判断要跳转的该图是否存在")
    public boolean graphIsExisted(@RequestParam("graphId") Long graphId) {
        return graphDAO.existsById(graphId);
    }

    /**
     * 获取系统支持的framework以及version
     * @return 系统支持的framework以及version
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/getVersionList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取系统支持的framework以及version")
    public Map<String, ArrayList<String>> getVersionList() {
        return FrameworkUtil.getVersionList();
    }

    /**
     * 万能分页component搜索
     * @param namespace 命名空间
     * @param type 类型
     * @param privacy public/private，为 public时，传回公共数据，private传回用户数据
     * @param desc 描述
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 更新结束时间
     * @param name 名字
     * @param pageNum 页数
     * @param pageSize 页面大小
     * @param principal 包含用户信息的对象
     * @return 搜索到的页面组件信息
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/resource/component/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页component搜索")
    public Page<ComponentVO> searchComponentPage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                 @RequestParam(value = "type", required = false, defaultValue = "") String type,
                                                 @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                 @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                                 @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                 @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                 @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                 @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        //获取用户名
        String userName = principal.getName();
        //获取页面信息
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //获取页面上的组件
        Page<Component> componentsPage = componentDAO.searchComponent(namespace, type, privacy, desc, startUpdateTime, endUpdateTime, name, userName, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ComponentVOEntityMapper.MAPPER::component2ComponentVO, componentsPage);
    }
    //更新上传管理页面用户拥有的组件的描述信息
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/changeComponent", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改上传管理页面用户拥有的组件的描述信息")
    public ResponseResult changeComponent(@MultiRequestBody("component") ComponentVO componentVO) throws JsonProcessingException, URISyntaxException {
        componentService.updateDesc(componentVO);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }
}
