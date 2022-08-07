package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.common.notifyEnum.MessageType;
import aizoo.domain.UserNotify;
import aizoo.repository.UserNotifyDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.NoticeService;
import aizoo.viewObject.object.ConversationVO;
import aizoo.viewObject.object.NotifyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@BaseResponse
@ResponseBody
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    @Autowired
    private UserNotifyDAO userNotifyDAO;

    private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);

    /**
     * 用户使用该接口发送私信给另一用户
     *
     * @param messageBody 私信内容
     * @param image
     * @param messageType 私信类型, 可选值: TEXT/SHARE/GRAPH
     * @param recipient 私信接收用户
     * @param principal 私信发送人， 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": {"messageId": "发送信息的id"}}
     */
    @RequestMapping(value = "/api/message/sendMessage",method = RequestMethod.POST)
    @WebLog(description = "发送私信给另一个用户")
    public ResponseResult sendMessage(@MultiRequestBody(value = "messageBody", required = false) String messageBody,
                            @MultiRequestBody(value = "image", required = false) String image,
                            @MultiRequestBody(value = "messageType") String messageType,
                            @MultiRequestBody(value = "toUsername") String recipient, Principal principal){
        try{
            Long id = noticeService.sendMessages(messageBody, MessageType.valueOf(messageType), principal.getName(), recipient);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), new HashMap<String,Long>(){{put("messageId", id);}});
        }catch (Exception e){
            logger.error("sendMessage Failed！");
            e.printStackTrace();
            return new ResponseResult(ResponseCode.SEND_MESSAGE_ERROR.getCode(), ResponseCode.SEND_MESSAGE_ERROR.getMsg(), null);
        }
    }

    /**
     * 获取用户的会话列表
     *
     * @param pageNum 页码
     * @param pageSize 页容量
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "List<ConversationVO>会话列表"}
     */
    @RequestMapping(value = "/api/message/conversationList",method = RequestMethod.GET)
    @WebLog(description = "获取用户的会话列表")
    public ResponseResult getConversationList(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal){
        try{
            List<ConversationVO> conversationVOList = noticeService.getConversationList(pageNum, pageSize, principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), conversationVOList);
        }catch (Exception e){
            logger.error("getConversationList Failed！");
            logger.error(e.getMessage(),e);
            return new ResponseResult(ResponseCode.GET_CONVERSATION_LIST_ERROR.getCode(), ResponseCode.GET_CONVERSATION_LIST_ERROR.getMsg(), null);
        }
    }

    /**
     * 获取用户与指定通信用户间的id小于指定值的历史私信记录
     *
     * @param fromUsername 指定的通信用户名
     * @param beforeId 私信id, 返回的私信记录id应该小于该id
     * @param size 一次请求私信的最大容量
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "Map<String, Object>类型,格式为{"messages": "List<MessageVO>类型历史消息列表", "beforeId": "更新后的 beforeId", "isEnd": "是否到达消息记录首部（到达首部则无更久远历史消息）"}
     */
    @RequestMapping(value = "/api/message/getHistoryMessages",method = RequestMethod.GET)
    @WebLog(description = "获取会话中的消息列表")
    public ResponseResult getMessages( @RequestParam(value = "fromUsername") String fromUsername,
                                       @RequestParam(value = "beforeId", required = false) Long beforeId,
                                       @RequestParam(value = "size", defaultValue = "10") Integer size, Principal principal){
        try{
            Map<String, Object> messageListInfo = noticeService.getMessageList(beforeId, size, fromUsername, principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), messageListInfo);
        }catch (Exception e){
            logger.error("getMessages Failed！");
            logger.error(e.getMessage(),e);
            return new ResponseResult(ResponseCode.GET_MESSAGE_LIST_ERROR.getCode(), ResponseCode.GET_MESSAGE_LIST_ERROR.getMsg(), null);
        }
    }

    /**
     * 用户发布公告
     *
     * @param title 公告标题
     * @param content 公告内容
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "null"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/admin/bulletin/create", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "管理员发布公告")
    public ResponseResult createAnnounce(@MultiRequestBody(value = "title") String title, @MultiRequestBody(value = "content") String content, Principal principal){
            noticeService.makeAnnounce(title, content, principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 用户拉取公告信息(将管理员最新发布的公告拉到用户自己的消息列表中之后，才可以在消息列表查询到公告记录信息)
     *
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "null"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/bulletin/pull", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "用户拉取公告信息")
    public ResponseResult pullAnnounce(Principal principal){
        try{
            noticeService.pullAnnounce(principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
        }catch (Exception e){
            logger.error("pullAnnounce Failed！");
            e.printStackTrace();
            return new ResponseResult(ResponseCode.PULL_ANNOUNCE_FAILED.getCode(), ResponseCode.PULL_ANNOUNCE_FAILED.getMsg(), null);
        }
    }

    /**
     * 用户获取指定页码内的公告列表
     *
     * @param pageNum 页码
     * @param pageSize 页容量
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "Map<String, Object>类型, 格式: {"totalNum": "总公告数", "NumOfThisMonth": "最近一个月内公告数", "unreadCount": "未读公告数", "announcePage": "Page<NotifyVO>类型分页公告列表"}"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/bulletin/bulletinList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "用户获取所有的公告列表")
    public ResponseResult getAnnounceList(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal){
        Page<NotifyVO> notifyVOPage = noticeService.getAnnounceList(pageNum, pageSize, principal.getName());
        Map<String, Object> announceInfo = noticeService.getAnnounceInfo(principal.getName());
        announceInfo.put("announcePage", notifyVOPage);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), announceInfo);
    }


    /**
     * 将指定公告置为已读
     *
     * @param id 公告id
     * @return {"code": "状态码", "msg": "返回信息", "data": "null"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/bulletin/read", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "用户查看公告内容，将公告置为已读")
    public ResponseResult getAnnounceList(@MultiRequestBody long id){
        UserNotify myNotify = userNotifyDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());
        myNotify.setIsRead(true);
        userNotifyDAO.save(myNotify);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 获取用户未读公告、通知、私信数量
     *
     * @param principal 当前用户
     * @return {"code": "状态码", "msg": "返回信息", "data": "HashMap<String, Integer>类型, 格式: {"bulletin": "未读公告数", "notification": "未读通知数", "message": "未读私信数"}"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/notice/unreadCount", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "统计消息系统的未读消息数量")
    public ResponseResult noticeUnreadCount(Principal principal){
        Map<String, Integer> unreadCount = noticeService.countUnreadNotice(principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), unreadCount);
    }
}
