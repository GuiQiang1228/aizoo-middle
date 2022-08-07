package aizoo.service;

import aizoo.common.GraphType;
import aizoo.common.exception.EmailNotExitingException;
import aizoo.common.exception.ForkNotAllowedException;
import aizoo.common.exception.ResourceSharedException;
import aizoo.common.notifyEnum.NotifyType;
import aizoo.common.notifyEnum.ShareableResource;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.mapper.ShareRecordVOEntityMapper;
import aizoo.viewObject.mapper.UserNotify2NotifyVOMapper;
import aizoo.viewObject.object.NotifyVO;
import aizoo.viewObject.object.ShareRecordVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ShareService {
    private final static Logger logger = LoggerFactory.getLogger(ShareService.class);

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
    private NotifyDAO notifyDAO;

    @Autowired
    private UserNotifyDAO userNotifyDAO;

    /**
     * 获取分享记录 作为发送者，获取自己给别人的分享记录；作为接收者，获取别人给自己的分享记录
     *
     * @param pageNum  消息提醒记录的页数
     * @param pageSize 分页大小
     * @param username 用户名
     * @param isSender 是否是发送者
     * @return shareRecordVOPage 根据限制条件搜索到的ShareRecordVOPage分页
     */
    public Page<ShareRecordVO> getShareRecord(Integer pageNum, Integer pageSize, String username, boolean isSender) {
        logger.info("Start get Share Record");
        logger.info("getShareRecord pageNum:{},pageSize:{},username:{},isSender:{}", pageNum, pageSize, username, isSender);

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ShareRecord> shareRecordPage = null;
        if (isSender)
            shareRecordPage = shareRecordDAO.findBySenderUsername(username, pageable);
        else
            shareRecordPage = shareRecordDAO.findByRecipientUsername(username, pageable);
        List<ShareRecordVO> shareRecordVOList = new ArrayList<>();
        for (ShareRecord shareRecord : shareRecordPage.getContent()) {
            ShareRecordVO shareRecordVO = ShareRecordVOEntityMapper.MAPPER.shareRecord2ShareRecordVO(shareRecord, daoUtil);
            shareRecordVOList.add(shareRecordVO);
        }
        if (!isSender)  // 如果是接收者，需要将成功获取到的分享记录都设为已读
            for (ShareRecord shareRecord : shareRecordPage.getContent()) {
                if (shareRecord.getIsRead() == false) {
                    shareRecord.setIsRead(true);
                    shareRecordDAO.save(shareRecord);
                }
            }
        Page<ShareRecordVO> shareRecordVOPage = new PageImpl<>(shareRecordVOList, shareRecordPage.getPageable(), shareRecordPage.getTotalElements());

        logger.info("getShareRecord return:{}", shareRecordVOPage);
        logger.info("End get Share Record");
        return shareRecordVOPage;
    }

    /**
     * 获取消息提醒记录的分页
     *
     * @param pageNum  消息提醒记录的页数
     * @param pageSize 分页大小
     * @param username 当前用户姓名
     * @return notifyVOPage 根据限制条件搜索到的NotifyVO分页
     */
    @Transactional
    public Page<NotifyVO> getShareRemind(Integer pageNum, Integer pageSize, String username) {
        logger.info("Start get Share Remind");
        logger.info("getShareRemind pageNum:{},pageSize{},username:{}", pageNum, pageSize, username);

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<UserNotify> userNotifyPage = userNotifyDAO.findByUserUsernameAndNotifyType(username, NotifyType.SHARE, pageable);
        // 对于取出来的消息记录，全部设为已读
        List<UserNotify> notifies = userNotifyPage.getContent();
        notifies.stream().forEach(n -> {
            n.setIsRead(true);
            userNotifyDAO.save(n);
        });

        Page<NotifyVO> notifyVOPage = VO2EntityMapper.mapEntityPage2VOPage(UserNotify2NotifyVOMapper.MAPPER::userNotify2NotifyVO, userNotifyPage);

        logger.info("notifyVOPage return:{}", notifyVOPage);
        logger.info("End get Share Remind");
        return notifyVOPage;
    }


    /**
     * 分享资源
     *
     * @param email           用户的邮箱地址
     * @param resourceId      分享资源的ID
     * @param resourceTypeStr 分享资源的类型，类型有数据资源，算子，复合组件图，实验图，服务图，应用图
     * @param resourceName    分享资源的名字
     * @param username        用户姓名
     * @return true 分享成功时，返回true; false 图是空图、图的component为空或者图的component的outputs为空时，返回false
     * @throws EmailNotExitingException
     * @throws ForkNotAllowedException
     * @throws ResourceSharedException
     */
    @Transactional
    public boolean shareResource(String email, Long resourceId, String resourceTypeStr, String resourceName, String username) throws EmailNotExitingException, ForkNotAllowedException, ResourceSharedException {
        logger.info("Start share Resource");
        logger.info("shareResource email:{},resourceId{},resourceTypeStr{},resourceName:{},username{}", email, resourceId, resourceTypeStr, resourceName, username);

        // 找到收件人
        User recipient = userDAO.findByEmail(email);
        if (recipient == null)
            // 不存在收件人时，throw EmailNotExitingException
            throw new EmailNotExitingException();
        ShareableResource resourceType = ShareableResource.valueOf(resourceTypeStr);

        // checkId用于检查当前资源食肉可以被fork
        Long checkId = resourceId;
        logger.info("验证是否是可以被fork的资源...");
        if (resourceType == ShareableResource.SERVICE_GRAPH) {
            Graph graph = graphDAO.findById(resourceId).orElseThrow(() -> new EntityNotFoundException());
            if (!graph.getUser().getUsername().equals(username)) {
                throw new ResourceSharedException();
            }
            checkId = graph.getService().getId();
        } else if (resourceType == ShareableResource.COMPONENT_GRAPH) {
            Graph graph = graphDAO.findById(resourceId).orElseThrow(() -> new EntityNotFoundException());
            if (!graph.getUser().getUsername().equals(username)) {
                // 图的所有者与当前用户的姓名不匹配时，throw ResourceSharedException
                throw new ResourceSharedException();
            }
            checkId = graph.getComponent().getId();
        }
        if (resourceType == ShareableResource.COMPONENT_GRAPH || resourceType == ShareableResource.APPLICATION_GRAPH || resourceType == ShareableResource.EXPERIMENT_GRAPH || resourceType == ShareableResource.SERVICE_GRAPH) {
            Graph graph = graphDAO.findById(resourceId).orElseThrow(() -> new EntityNotFoundException());
            if (graph.getNodeList().equals("[]") || (graph.getComponent() != null && graph.getComponent().getOutputs().size() == 0))
                return false;
        }
        if (!componentService.allowedFork(checkId, resourceType.getType()))
            // 图不允许fork，throw ForkNotAllowedException
            throw new ForkNotAllowedException();

        ShareRecord shareRecord = new ShareRecord(userDAO.findByUsername(username), userDAO.findByEmail(email), resourceType, resourceId, false, false);
        shareRecordDAO.save(shareRecord);

        //生成一条分享的消息记录
        logger.info("生成一条分享的消息记录...");
        String senderEmail = userDAO.findByUsername(username).getEmail();
        if (senderEmail == null)
            senderEmail = "暂无邮箱";

        Notify notify = new Notify(userDAO.findByUsername(username), NotifyType.SHARE);
        notify.setContent(String.format("%s (%s) 给您分享了一个%s,请查收", username, senderEmail, resourceType.getValue()));
        notify.setTitle(String.format("%s : %s", resourceType.getValue(), resourceName));
        notifyDAO.save(notify);
        UserNotify userNotify = new UserNotify(recipient, notify, false);
        userNotifyDAO.save(userNotify);
        logger.info("Stop share Resource");
        return true;
    }

    /**
     * 删除分享记录
     */
    @Transactional
    public void deleteShareRecord(Long id, boolean clear, String username) {
        logger.info("Start delete Share Record");
        logger.info("deleteShareRecord id:{},clear:{},username{}", id, clear, username);
        if (!clear) {
            shareRecordDAO.deleteById(id);
        } else {
            List<ShareRecord> records2Delete = shareRecordDAO.findByRecipientUsername(username);
            shareRecordDAO.deleteInBatch(records2Delete);
        }
        logger.info("End delete Share Record");
    }

    /**
     * 删除消息提醒
     *
     * @param id       消息id
     * @param clear    判断是否清空消息列表，如果是true，清空所有消息，如果是false，只删除消息ID为id的这条消息
     * @param username 用户姓名
     */
    @Transactional
    public void deleteShareRemind(Long id, boolean clear, String username) {
        logger.info("Start delete Share Remind");
        logger.info("deleteShareRecord id:{},clear:{},username{}", id, clear, username);
        if (!clear) {
            userNotifyDAO.deleteUserNotifyById(id);
        } else {
            userNotifyDAO.deleteAllByUserUsernameAndNotifyType(username, NotifyType.SHARE);
        }
        logger.info("End delete Share Remind");
    }
}
