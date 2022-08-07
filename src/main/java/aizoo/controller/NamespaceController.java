package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.*;
import aizoo.repository.ComponentDAO;
import aizoo.repository.NamespaceDAO;
import aizoo.repository.ServiceDAO;
import aizoo.repository.UserDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.NamespaceService;
import aizoo.utils.NamespaceUtil;
import aizoo.viewObject.mapper.NamespaceVOEntityMapper;
import aizoo.viewObject.object.NamespaceVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BaseResponse
@RestController
public class NamespaceController {

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    private ComponentDAO componentDAO;

    /**
     * 注册命名空间
     *
     * @param namespace  命名空间
     * @param privacy    注册的命名空间的privacy
     * @param principal  主体
     * @return ResponseResult类型，为注册命名空间的结果
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/namespaceRegister", method = RequestMethod.POST)
    @WebLog(description = "注册命名空间")
    public ResponseResult registerNamespace(@MultiRequestBody String namespace,
                                            @MultiRequestBody String privacy ,Principal principal) {
        //1. 根据主体的用户名获取user
        User user = userDAO.findByUsername(principal.getName());
        //添加命名空间，result对添加命名空间返回的结果
        String result = namespaceService.addNamespace(user, privacy, namespace);

        //2. 返回结果
        if(result.equals("SUCCESS")){
            //生成namespace和privacy的映射map
            Map<String,String> map = new HashMap<>();
            map.put("namespace", namespace);
            map.put("privacy", privacy);

            //根据SUCCESS的code和msg以及map创建ResponseResult并返回
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), map);
        }
        else //若添加命名空间失败，根据错误代码和信息创建ResponseResult并返回
            return new ResponseResult(ResponseCode.NAMESPACE_REGISTER_ERROR.getCode(),
                                     result, null);
    }

    /**
     * fork注册命名空间
     *
     * @param id             service的Id
     * @param isFirstAtom    当前组件是否为单次fork的原子组件
     * @param principal      主体
     * @return ResponseResult类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/forkNamespaceRegister", method = RequestMethod.POST)
    @WebLog(description = "fork注册命名空间")
    public ResponseResult forkRegisterNamespace(@MultiRequestBody long id ,@MultiRequestBody boolean isFirstAtom, Principal principal) {
        //1. 设置命名空间的privacy为private
        String privacy = "private";

        //根据id获取service实体，获取失败则抛出异常
        Service service = serviceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        //根据主体的用户名获取到user实体
        User user = userDAO.findByUsername(principal.getName());
        //获取service的原有username和namespace
        String sourceUsername = service.getUser().getUsername();
        String namespace = service.getNamespace().getNamespace();

        //2. 获取fork的命名空间
        String forkNamespace = namespaceService.getForkNamespace(user, sourceUsername, namespace, isFirstAtom);

        //3. 添加命名空间，得到result
        String result = namespaceService.addNamespace(user, privacy, forkNamespace);

        //4. 返回结果
        if(result.equals("SUCCESS")){
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), forkNamespace);
        }
        else
            return new ResponseResult(ResponseCode.NAMESPACE_REGISTER_ERROR.getCode(),
                    result, null);
    }

    /**
     * 获取用户的命名空间分页
     *
     * @param pageNum     当前页
     * @param pageSize    每页显示的条数
     * @return Page<NamespaceVO>类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/namespace", method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户的命名空间分页")
    public Page<NamespaceVO> getNamespacePage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        //1. 获取当前用户的authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //获取当前用户的username
        String userName = authentication.getName();
        //根据当前页和页面大小，并按照updateTime降序，获得分页pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");

        //2. 对用户命名空间{username}.checkPoint进行模糊查询，返回分页的结果
        Page<Namespace> namespacePage = namespaceDAO.findByUserUsernameAndNamespaceNotLike(userName,userName+".checkPoint%",pageable);

        //3. 将namespace分页转换成namespaceVO分页，返回结果
        return VO2EntityMapper.mapEntityPage2VOPage(NamespaceVOEntityMapper.MAPPER::namespace2NamespaceVO,namespacePage);
    }

    /**
     * 管理员获取所有用户的命名空间分页
     *
     * @param pageNum     当前页
     * @param pageSize    每页显示的条数
     * @param principal   主体
     * @return Page<NamespaceVO>类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/resource/namespace", method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "管理员获取所有用户的命名空间分页")
    public Page<NamespaceVO> getAllNamespacePage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,Principal principal) {
        //1. 根据当前页和页面大小，并按照updateTime降序，获得分页pageable对象
        Pageable pageable  = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");

        //2. 对所有用户的命名空间{username}.checkPoint进行模糊查询，返回分页的结果
        Page<Namespace> allNamespacePage = namespaceDAO.findByNamespaceNotLike(principal.getName()+".checkPoint%",pageable);

        //3. 将namespace分页转换成namespaceVO分页，返回结果
        return VO2EntityMapper.mapEntityPage2VOPage(NamespaceVOEntityMapper.MAPPER::namespace2NamespaceVO,allNamespacePage);
    }

    /**
     * 获取用户组件的命名空间列表
     *
     * @param type         需要获取的namespace类型
     * @param principal    主体
     * @return List<Map<String,String>>类型，Map格式为：{"namespace":"","privacy":"namespace对应的privacy"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/component/getNamespaceList", method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户组件的命名空间列表")
    public List<Map<String,String>> getComponentNamespaceList(@RequestParam(value = "type",defaultValue = "all") String type,
                                                              Principal principal){
        //1. 获取当前用户的username
        String username = principal.getName();

        //获取用户命名空间列表
        List<Namespace> namespaceList = namespaceService.getNamespaceListByType(username, type);

        //2. 获取命名空间namespace和privacy的映射关系列表并返回
        return NamespaceUtil.formatNamespaceList(namespaceList);
    }

    /**
     * 获取用户数据资源的命名空间列表
     *
     * @param principal      主体
     * @return List<Map<String,String>>类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/datasource/getNamespaceList", method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户数据资源的命名空间列表")
    public List<Map<String,String>> getDatasourceNamespaceList(Principal principal){
        //1. 获取当前用户的username
        String username = principal.getName();

        //获取按照namespace升序排列的用户数据资源的命名空间列表
        Sort sort = Sort.by(Sort.Direction.ASC,"namespace");
        List<Namespace> namespaceList = namespaceDAO.findByUserUsernameAndNamespaceLike(username, username + ".datasource%", sort);

        //2. 将不合法的命名空间，即长度小于4的命名空间剔除
        namespaceList = namespaceService.getFilterNamespaceList(namespaceList);

        //3. 获取命名空间namespace和privacy的映射关系列表并返回
        return NamespaceUtil.formatNamespaceList(namespaceList);
    }

    /**
     * 修改组件或命名空间的访问权限
     *
     * @param namespace  需要修改的命名空间
     * @param type       需要修改的类型
     * @param privacy    需要将访问权限修改为privacy
     * @param id         需要修改的组件的Id
     * @return ResponseResult类型，修改结果
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/privacyEdit", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改组件或命名空间的访问权限")
    public ResponseResult privacyEdit(@MultiRequestBody String namespace, @MultiRequestBody String type,
                               @MultiRequestBody String privacy, @MultiRequestBody long id) {
        //1. 修改组件或命名空间的访问权限为privacy
        String result = namespaceService.modifyPrivacy(namespace, type, privacy, id);

        //2. 返回结果
        if(!result.equals("SUCCESS"))
            return new ResponseResult(ResponseCode.PRIVACY_MODIFY_ERROR.getCode(), result, false);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), true);
    }

    /**
     * 返回不能公开的命名空间/组件的所有上层私有命名空间
     *
     * @param namespace
     * @return ResponseResult类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/privacyEditList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "返回不能公开的命名空间/组件的所有上层私有命名空间")
    public ResponseResult privacyEditList(@MultiRequestBody String namespace) {
        //1. 获取当前命名空间的所有上层命名空间
        List result = namespaceService.getPrivacyEditList(namespace);
        //2. 返回结果
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }

    /**
     * 一键公开
     *
     * @param namespace   需要公开的命名空间
     * @param type        需要公开的类型
     * @param id          需要公开的组件的Id
     * @return ResponseResult类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/privacyEditPublic", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "一键公开")
    public ResponseResult privacyEditPublic(@MultiRequestBody String namespace, @MultiRequestBody String type,
                                            @MultiRequestBody long id) {
        //将type对应的组件/命名空间公开
        namespaceService.privacyEditPublic(namespace, type, id);
        //返回结果
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 万能分页namespace搜索
     *
     * @param namespace         命名空间
     * @param type              当前组件是否为单次fork的原子组件
     * @param privacy           namespace的访问权限
     * @param startUpdateTime   namespace的update_time起始
     * @param endUpdateTime     namespace的update_time终止
     * @param pageNum           当前页
     * @param pageSize          每页显示的条数
     * @param principal         主体
     * @return Page<NamespaceVO>类型
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/resource/namespace/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页namespace搜索")
    public Page<NamespaceVO> searchNamespacePage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                 @RequestParam(value = "type", required = false, defaultValue = "") String type,
                                                 @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                 @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                 @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        //1. 获取当前用户的username
        String userName = principal.getName();
        //根据当前页和页面大小，获得分页pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        //2. 获得命名空间分页结果namespacePage
        Page<Namespace> namespacePage = namespaceDAO.searchNamespace(namespace, type, privacy, startUpdateTime, endUpdateTime, userName, pageable);

        //3. 将namespace分页转换成namespaceVO分页，返回结果
        return VO2EntityMapper.mapEntityPage2VOPage(NamespaceVOEntityMapper.MAPPER::namespace2NamespaceVO, namespacePage);
    }
}
