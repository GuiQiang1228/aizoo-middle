package aizoo.service;

import aizoo.common.notifyEnum.MessageStatus;
import aizoo.common.notifyEnum.MessageType;
import aizoo.common.notifyEnum.NotifyType;
import aizoo.domain.Conversation;
import aizoo.domain.Message;
import aizoo.domain.Notify;
import aizoo.domain.UserNotify;
import aizoo.repository.*;
import aizoo.viewObject.mapper.ConversationVOEntityMapper;
import aizoo.viewObject.mapper.MessageVOEntityMapper;
import aizoo.viewObject.mapper.UserNotify2NotifyVOMapper;
import aizoo.viewObject.object.ConversationVO;
import aizoo.viewObject.object.MessageVO;
import aizoo.viewObject.object.NotifyVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.*;

@Service
public class NoticeService {
    @Autowired
    private UserDAO userDAO;

    @Autowired
    private MessageDAO messageDAO;

    @Autowired
    private ConversationDAO conversationDAO;

    @Autowired
    private NotifyDAO notifyDAO;
    
    @Autowired
    private UserNotifyDAO userNotifyDAO;

    @Autowired
    private ShareRecordDAO shareRecordDAO;

    private final static Logger logger = LoggerFactory.getLogger(NoticeService.class);


    /**
     * 根据用户名、分页信息获取当前用户历史会话列表
     *
     * @param pageNum 页码
     * @param pageSize 页容量
     * @param username 用户名
     * @return List<ConversationVO>
     */
    @Transactional
    public List<ConversationVO> getConversationList(Integer pageNum, Integer pageSize, String username){
        logger.info("Start get Conversation List");
        logger.info("getConversationList pageNum:{},pageSize:{},username:{}",pageNum,pageSize,username);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");
        Page<Conversation> conversationPage =  conversationDAO.findByOwnerUsername(username, pageable);
        List<ConversationVO> conversationVOList = new ArrayList<>();
        for(Conversation conversation : conversationPage.getContent()){
            ConversationVO conversationVO = ConversationVOEntityMapper.MAPPER.conversation2ConversationVO(conversation, messageDAO);
            conversationVOList.add(conversationVO);
        }
        logger.info("getConversationList return:{}",conversationVOList);
        logger.info("End get Conversation List");
        return conversationVOList;
    }

    /**
     * 根据消息发送方用户名、消息类型向消息接受方发送指定消息内容
     *
     * @param content 消息内容
     * @param messageType 消息类型,可选值: TEXT/SHARE/GRAPH
     * @param username 消息发送方
     * @param recipient 消息接收方
     * @return Long类型: 发送消息内容对应id
     */
    @Transactional
    public Long sendMessages(String content, MessageType messageType, String username, String recipient){
        logger.info("Start send Messages");
        logger.info("sendMessages Message:content={},messageType={},username={},recipient={}",content, messageType,username,recipient);
        Message message = new Message(content, messageType, userDAO.findByUsername(username), userDAO.findByUsername(recipient));
        Date nowTime = new Date();
        // conversation1: conversation2:
        Conversation conversation1 = conversationDAO.findByOwnerUsernameAndParticipantUsername(username, recipient);
        Conversation conversation2 = conversationDAO.findByOwnerUsernameAndParticipantUsername(recipient, username);
        // 更新最近会话时间？
        conversation1.setUpdateTime(nowTime);
        conversationDAO.save(conversation1);
        // 给本条message建立两个用户会话之间的对应关系
        message.setConversations(Arrays.asList(conversation1, conversation2));
        //
        message.setCreateTime(nowTime);
        message.setUpdateTime(nowTime);
        message.setMessageStatus(MessageStatus.SUCCESS);
        messageDAO.save(message);
        conversation2.setUpdateTime(nowTime);
        conversation2.setLatestMessage(message.getId());
        conversation2.setUnreadCount(conversation2.getUnreadCount() + 1);   // 会不会导致多线程数据不一致的情况？
        conversationDAO.save(conversation2);
        logger.info("sendMessages return:{}",message.getId());
        logger.info("End send Messages");
        return message.getId();
    }

    /**
     * 根据用户名、会话参与者获取指定私信id前的历史私信记录
     *
     * @param beforeId  请求的私信 Id 需要比 beforeId 小, 可以为 null
     * @param size  一次请求私信的最大容量
     * @param participant  私信会话的参与者的用户名
     * @param username   用户自己的用户名
     * @return Map<String,Object>类型，格式为{"messages": "List<MessageVO>类型历史消息列表", "beforeId": "更新后的 beforeId", "isEnd": "是否到达消息记录首部（到达首部则无更久远历史消息）"}
     */
    @Transactional
    public Map<String,Object> getMessageList(@Nullable Long beforeId, Integer size, String participant, String username){
        logger.info("Start get Message List");
        logger.info("getMessageList beforeId:{},size:{},participant:{},username:{}",beforeId,size,participant,username);
        Conversation conversation = conversationDAO.findByOwnerUsernameAndParticipantUsername(username, participant);
        Map<String,Object> result = new HashMap<>();
        if(beforeId == null){
            if(conversation.getLatestMessage() == null){  //如果是第一次请求消息，但是会话中没有一条消息
                result.put("messages", null);
                result.put("beforeId", null);
                result.put("isEnd", true);
                logger.info("getMessageList return:{}",result);
                logger.info("End get Message List");
                return result;
            }
            else {
                beforeId = conversation.getLatestMessage() + 1;  // 加一是为了结果包含最新的一条消息
                conversation.setUnreadCount(0);
            }
        }
        Pageable pageable = PageRequest.of(0, size, Sort.Direction.DESC, "id");
        Page<Message> messagePage = messageDAO.findMessagesByConversationsContainsAndIdIsLessThan(conversation, beforeId, pageable);
        Page<MessageVO> messageVOPage = VO2EntityMapper.mapEntityPage2VOPage(MessageVOEntityMapper.MAPPER::message2MessageVO, messagePage);
        List<MessageVO> messageVOList = messageVOPage.getContent();
        result.put("messages", messageVOList);
        // 若要请求更早的消息，则用lastId表示前端需要传给后台的beforeId
        result.put("beforeId", messageVOList.get(messageVOList.size()-1).getId());
        result.put("isEnd", messageVOPage.isLast());
        logger.info("getMessageList return:{}",result);
        logger.info("End get Message List");
        return result;
    }


    /**
     * 根据用户名、标题、内容添加指定公告记录
     *
     * @param title 公告标题
     * @param content 公告内容
     * @param username 用户名(公告发送者)
     */
    @Transactional
    public void makeAnnounce(String title, String content, String username){
        logger.info("Start make Announce");
        logger.info("makeAnnounce title:{},content:{},username:{}",title,content,username);
        Notify announce = new Notify(content, title, NotifyType.ANNOUNCE, userDAO.findByUsername(username));
        notifyDAO.save(announce);
        logger.info("End make Announce");
    }

    /**
     * 根据用户名为用户拉取最新公告信息
     * 将管理员最新发布的公告拉到用户自己的消息列表中之后，才可以在消息列表查询到公告记录信息
     *
     * @param username 用户名
     */
    @Transactional
    public void pullAnnounce(String username){
        logger.info("Start pull Announce");
        logger.info("pullAnnounce username:{}",username);
        // 获取用户已拉取的最新的公告
        UserNotify myLatestAnnounce = userNotifyDAO.findTopByUserUsernameAndNotifyType(username, NotifyType.ANNOUNCE, Sort.by(Sort.Direction.DESC, "id"));
        Date afterTime = null;
        // 如果用户的消息表中一条公告都还没有拉取过
        if(myLatestAnnounce == null){
            Notify earliestAnnounce = notifyDAO.findTopByTypeOrderByCreateTimeAsc(NotifyType.ANNOUNCE);
            // 如果一条公告都还没有被发布过，则不需要执行pull操作了
            if(earliestAnnounce == null) {
                logger.info("earliestAnnounce is null, End pull Announce");
                return;
            }
            else{
                afterTime = earliestAnnounce.getCreateTime();
                Calendar c = Calendar.getInstance();
                c.setTime(afterTime);
                c.add(Calendar.HOUR_OF_DAY, -1);  // 取第一条公告的前一个小时作为基准时间
                afterTime = c.getTime();
            }
        }
        else
            afterTime = myLatestAnnounce.getNotify().getCreateTime();
        // 为用户拉取指定时间后的公告
        List<Notify> newAnnounces = notifyDAO.findByTypeAndCreateTimeAfter(NotifyType.ANNOUNCE, afterTime, Sort.by(Sort.Direction.ASC, "createTime"));
        for (Notify announce: newAnnounces){
            UserNotify userNotify = new UserNotify(userDAO.findByUsername(username), announce, false);
            userNotifyDAO.save(userNotify);
        }
        logger.info("End pull Announce");
    }

    /**
     * 根据用户名获取当前页码内的公告列表
     *
     * @param pageNum 页码
     * @param pageSize 页容量
     * @param username 用户名
     * @return Page<NotifyVO>分页公告列表
     */
    public Page<NotifyVO> getAnnounceList(Integer pageNum, Integer pageSize, String username){
        logger.info("Start get Announce List");
        logger.info("getAnnounceList pageNum:{},pageSize:{},username{}",pageNum,pageSize,username);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserNotify> userNotifyPage = userNotifyDAO.findByUserUsernameAndNotifyType(username, NotifyType.ANNOUNCE, pageable);
        Page<NotifyVO> notifyVOPage = VO2EntityMapper.mapEntityPage2VOPage(UserNotify2NotifyVOMapper.MAPPER::userNotify2NotifyVO, userNotifyPage);
        logger.info("getAnnounceList return:{}",notifyVOPage);
        logger.info("End get Announce List");
        return notifyVOPage;
    }


    /**
     * 根据用户名获取总公告数、最近一个月内公告数、当前用户未读公告数信息
     *
     * @param username
     * @return HashMap<String, Object>类型, 格式： {"totalNum": "总公告数", "NumOfThisMonth": "最近一个月内公告数", "unreadCount": "未读公告数"}
     */
    public Map<String, Object> getAnnounceInfo(String username){
        logger.info("Start get Announce Info,获取关于公告的信息");
        logger.info("getAnnounceInfo username:{}",username);
        Integer totalNum = notifyDAO.countByType(NotifyType.ANNOUNCE);
        Date nowTime = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(nowTime);
        c.add(Calendar.MONTH, -1);  // 取现在的前一个月作为起始日期
        Integer NumOfThisMonth = notifyDAO.countByTypeAndCreateTimeAfter(NotifyType.ANNOUNCE, c.getTime());
        Integer unreadCount = userNotifyDAO.countByUserUsernameAndNotifyTypeAndIsRead(username, NotifyType.ANNOUNCE, false);
        logger.info("getAnnounceInfo return: totalNum={},NumOfThisMonth={},unreadCount={}",totalNum,NumOfThisMonth,unreadCount);
        logger.info("End get Announce Info");
        return new HashMap<String, Object>(){{
            put("totalNum", totalNum);
            put("NumOfThisMonth", NumOfThisMonth);
            put("unreadCount", unreadCount);
        }};
    }

    /**
     * 根据用户名获取未读公告、通知、私信数量
     *
     * @param username 用户名
     * @return HashMap<String, Integer>类型, 格式: {"bulletin": "未读公告数", "notification": "未读通知数", "message": "未读私信数"}
     */
    public Map<String, Integer> countUnreadNotice(String username){
        logger.info("Start count Unread Notice");
        logger.info("countUnreadNotice username:{}",username);
        Integer announceCount = userNotifyDAO.countByUserUsernameAndNotifyTypeAndIsRead(username, NotifyType.ANNOUNCE, false);
        Integer notificationCount = userNotifyDAO.countByUserUsernameAndNotifyTypeNotAndIsRead(username, NotifyType.ANNOUNCE, false);
        Integer messageCount = conversationDAO.countByOwnerUsernameAndUnreadCountGreaterThan(username, Integer.valueOf(0));
        logger.info("countUnreadNotice return: bulletin={},notification={},message={}",announceCount,notificationCount,messageCount);
        logger.info("End count Unread Notice");
        return new HashMap<String, Integer>(){{
                put("bulletin", announceCount);
                put("notification", notificationCount);
                put("message", messageCount);
            }
        };
    }


}
