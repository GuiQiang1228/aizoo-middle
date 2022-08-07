package aizoo.viewObject.mapper;

import aizoo.domain.Notify;
import aizoo.domain.UserNotify;
import aizoo.viewObject.object.NotifyVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserNotify2NotifyVOMapper {

    UserNotify2NotifyVOMapper MAPPER = Mappers.getMapper(UserNotify2NotifyVOMapper.class);

    NotifyVO userNotify2NotifyVO(UserNotify userNotify);

    @AfterMapping
    default void completeNotifyVO(@MappingTarget NotifyVO notifyVO, UserNotify userNotify){
        Notify notify = userNotify.getNotify();
        notifyVO.setContent(notify.getContent());
        notifyVO.setTitle(notify.getTitle());
        notifyVO.setSender(notify.getSender().getUsername());
        notifyVO.setType(notify.getType());
        notifyVO.setIsRead(userNotify.getIsRead());
        notifyVO.setCreateTime(notify.getCreateTime());
        notifyVO.setUpdateTime(notify.getUpdateTime());
    }

}
