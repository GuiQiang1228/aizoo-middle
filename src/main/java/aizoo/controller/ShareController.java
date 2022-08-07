package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.aspect.WebLog;
import aizoo.common.GraphType;
import aizoo.common.ResourceType;
import aizoo.common.exception.EmailNotExitingException;
import aizoo.common.exception.ForkNotAllowedException;
import aizoo.common.exception.ResourceSharedException;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ComponentService;
import aizoo.service.ShareService;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.mapper.ShareRecordVOEntityMapper;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.NotifyVO;
import aizoo.viewObject.object.ShareRecordVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@ResponseBody
@BaseResponse
public class ShareController {

    @Autowired
    private ShareService shareService;

    @Autowired
    private ShareRecordDAO shareRecordDAO;

    @Autowired
    private DAOUtil daoUtil;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    private UserNotifyDAO userNotifyDAO;

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);


    /**
     * 用户获取分享的提醒列表
     *
     * @param pageNum   消息提醒记录的页数
     * @param pageSize  分页大小
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: shareRemindPage}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/notification/shareNotification", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "用户获取分享的提醒列表")
    public ResponseResult getShareRemind(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        Page<NotifyVO> shareRemindPage = shareService.getShareRemind(pageNum, pageSize, principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), shareRemindPage);
    }


    /**
     * 主动分享某个资源给其他用户
     *
     * @param email        用户的邮箱地址
     * @param resourceId   分享资源的ID
     * @param resourceName 分享资源的名字
     * @param resourceType 分享资源的类型，类型有数据资源，算子，复合组件图，实验图，服务图，应用图
     * @param principal    当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：{code: 10076, msg: "空图禁止分享", data: null} {code: 10071, msg: "该邮箱不存在", data: null} {code: 30010, msg: "fork的组件或者其子组件有缺失", data: null} {code: 10075, msg: "非本人资源，无分享权限", data: null}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/notification/share", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "主动分享某个资源给其它用户")
    public ResponseResult shareResource(@MultiRequestBody(value = "email") String email,
                                        @MultiRequestBody(value = "resourceId") long resourceId,
                                        @MultiRequestBody(value = "resourceName") String resourceName,
                                        @MultiRequestBody(value = "resourceType") String resourceType, Principal principal) {
        try {
            boolean flag = shareService.shareResource(email, resourceId, resourceType, resourceName, principal.getName());
            if (flag)
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
            else
                return new ResponseResult(ResponseCode.SHARE_NULL_ERROR.getCode(), ResponseCode.SHARE_NULL_ERROR.getMsg(), null);
        } catch (EmailNotExitingException e) {
            logger.error(e.getMessage());
            return new ResponseResult(ResponseCode.EMAIL_NOT_EXITING_ERROR.getCode(), ResponseCode.EMAIL_NOT_EXITING_ERROR.getMsg(), null);
        } catch (ForkNotAllowedException e) {
            logger.error(e.getMessage());
            return new ResponseResult(ResponseCode.FORK_NOT_EXIST.getCode(), ResponseCode.FORK_NOT_EXIST.getMsg(), false);
        } catch (ResourceSharedException e) {
            logger.error(e.getMessage());
            return new ResponseResult(ResponseCode.SHARE_OUT_OF_BOUNDS.getCode(), ResponseCode.SHARE_OUT_OF_BOUNDS.getMsg(), false);
        }
    }


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/share/details", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "查看分享资源的详情信息")
    public ResponseResult getShareDetail() {
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 删除分享消息提醒
     *
     * @param clear     判断是否清空消息列表，如果是true，清空所有消息，如果是false，只删除消息ID为id的这条消息
     * @param id        消息id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：{code: 10072, msg: "非本人资源，无法删除", data: null} {code: 10065, msg: "删除分享记录失败！" + 错误信息, data: null}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/notification/share/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除分享消息提醒")
    public ResponseResult deleteShareRemind(@MultiRequestBody boolean clear,
                                            @MultiRequestBody long id, Principal principal) {
        try {
            UserNotify un = userNotifyDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            if (!un.getUser().getUsername().equals(principal.getName())) {
                return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
            }
            shareService.deleteShareRemind(id, clear, principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
        } catch (Exception e) {
            logger.info(e.getMessage());
            return new ResponseResult(ResponseCode.DELETE_SHARE_RECORD_FAILED.getCode(), ResponseCode.DELETE_SHARE_RECORD_FAILED.getMsg() + e.getMessage(), null);
        }
    }


    /**
     * 接受其他用户的分享
     *
     * @param id        分享记录的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：{code: 30010, msg: "fork的组件或者其子组件有缺失", data: null} {code: 10067, msg:"接受分享失败", data: null}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/share/accept", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "接受其他用户的分享")
    @ResourceCheck(resourceTypes = {ResourceType.DISK})
    public ResponseResult acceptShare(@MultiRequestBody long id, Principal principal) {
        try {
            User user = userDAO.findByUsername(principal.getName());

            ShareRecord record = shareRecordDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            Map<String, Object> results = new HashMap<>();
            HashMap<Long, Long> forkedService = new HashMap<>();
            HashMap<Long, Long> forkedDatasource = new HashMap<>();
            HashMap<Long, Long> map = new HashMap<>();
            Long graphId = null;
            Long componentId = null;
            switch (record.getResourceType()) {
                case COMPONENT: {
                    Component component = componentDAO.findById(record.getResourceId()).orElseThrow(() -> new EntityNotFoundException());
                    // 如果不允许被fork，则抛出异常
                    if (!componentService.allowedFork(component.getId(), GraphType.COMPONENT.toString()))
                        throw new ForkNotAllowedException();

                    Component newComponent = componentService.newFork(user, component.getId(), "private", "accept component share", true, map, forkedService, forkedDatasource);
                    componentId = newComponent.getId();
                    results.put("namespace", newComponent.getNamespace().getNamespace());
                    results.put("resourceName", newComponent.getName());
                    break;
                }
                case COMPONENT_GRAPH: {
                    Graph graph = graphDAO.findById(record.getResourceId()).orElseThrow(() -> new EntityNotFoundException());
                    Component component = graph.getComponent();
                    // 如果不允许被fork，则抛出异常
                    if (!componentService.allowedFork(component.getId(), GraphType.COMPONENT.toString()))
                        throw new ForkNotAllowedException();

                    Component newComponent = componentService.newFork(user, component.getId(), "private", "accept component share", false, map, forkedService, forkedDatasource);
                    results.put("namespace", newComponent.getNamespace().getNamespace());
                    results.put("resourceName", newComponent.getName());
                    graphId = newComponent.getGraph().getId();
                    break;
                }
                case EXPERIMENT_GRAPH: {
                    if (!componentService.allowedFork(record.getResourceId(), GraphType.JOB.toString()))
                        throw new ForkNotAllowedException();
                    Graph newGraph = componentService.forkExperimentGraph(record.getResourceId(), principal.getName(), map, forkedService, forkedDatasource);
                    results.put("resourceName", newGraph.getName());
                    graphId = newGraph.getId();
                    break;
                }
                case APPLICATION_GRAPH: {
                    if (!componentService.allowedFork(record.getResourceId(), GraphType.APPLICATION.toString()))
                        throw new ForkNotAllowedException();
                    Graph newGraph = componentService.forkApplication(record.getResourceId(), principal.getName(), map, forkedService, forkedDatasource);
                    results.put("resourceName", newGraph.getName());
                    graphId = newGraph.getId();
                    break;
                }
                case SERVICE_GRAPH: {
                    Graph graph = graphDAO.findById(record.getResourceId()).orElseThrow(() -> new EntityNotFoundException());
                    Service service = graph.getService();
                    // 如果不允许被fork，则抛出异常
                    if (!componentService.allowedFork(service.getId(), GraphType.SERVICE.toString()))
                        throw new ForkNotAllowedException();

                    Service newService = componentService.forkService(service.getId(), principal.getName(), map, forkedService, forkedDatasource);
                    results.put("namespace", newService.getNamespace().getNamespace());
                    results.put("resourceName", newService.getName());
                    graphId = newService.getGraph().getId();
                    break;
                }
            }

            // 将分享记录置为已接受
            record.setAccepted(true);
            record.setGraphId(graphId);
            record.setComponentId(componentId);
            shareRecordDAO.saveAndFlush(record);

//            Map<String, Object> results = shareService.acceptShare(id, principal.getName(), user);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), results);
        } catch (ForkNotAllowedException e) {
            logger.info(e.getMessage());
            return new ResponseResult(ResponseCode.FORK_NOT_EXIST.getCode(), ResponseCode.FORK_NOT_EXIST.getMsg(), false);
        } catch (Exception e) {
            logger.info(e.getMessage());
            return new ResponseResult(ResponseCode.SHARE_ACCEPT_ERROR.getCode(), ResponseCode.SHARE_ACCEPT_ERROR.getMsg() + e.getMessage(), null);
        }
    }


    /**
     * 万能分页用户分享记录搜索
     *
     * @param name      全局模糊搜索关键字
     * @param isSender  是否是发送者 作为为发送者，获取自己给别人的分享记录；作为接收者，获取别人给自己的分享记录
     * @param pageNum   消息提醒记录的页数
     * @param pageSize  分页大小
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return shareRecordVOPage 根据限制条件搜索到的ShareRecordVOPage分页
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/share/search/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页用户分享记录搜索")
    public Page<ShareRecordVO> acceptShareSearchPage(@RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                     @RequestParam(value = "isSender", required = false, defaultValue = "") boolean isSender,
                                                     @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        String userName = principal.getName();
        Page<ShareRecord> shareRecordPage;
        if (isSender) {
            if (name.length() > 0)  //如果关键字不空，从发送记录全局模糊搜索关键字name
                shareRecordPage = shareRecordDAO.sendSearchShareRecord(name, userName, pageable);
            else   //如果关键字为空，返回所有主动分享记录列表
                return shareService.getShareRecord(pageNum, pageSize, userName, true);
        } else {
            if (name.length() > 0)  //如果关键字不空，从接收记录全局模糊搜索关键字name
                shareRecordPage = shareRecordDAO.acceptSearchShareRecord(name, userName, pageable);
            else  //如果关键字为空，返回所有接收分享记录列表
                return shareService.getShareRecord(pageNum, pageSize, userName, false);
        }
        List<ShareRecordVO> shareRecordVOList = new ArrayList<>();
        for (ShareRecord shareRecord : shareRecordPage.getContent()) {
            ShareRecordVO shareRecordVO = ShareRecordVOEntityMapper.MAPPER.shareRecord2ShareRecordVO(shareRecord, daoUtil);
            shareRecordVOList.add(shareRecordVO);
        }
        Page<ShareRecordVO> shareRecordVOPage = new PageImpl<>(shareRecordVOList, shareRecordPage.getPageable(), shareRecordPage.getTotalElements());
        return shareRecordVOPage;
    }
}
