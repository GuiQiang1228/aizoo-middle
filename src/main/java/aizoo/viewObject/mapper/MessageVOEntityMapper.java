package aizoo.viewObject.mapper;


import aizoo.domain.Message;
import aizoo.viewObject.object.MessageVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MessageVOEntityMapper {

    MessageVOEntityMapper MAPPER = Mappers.getMapper(MessageVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    target = "fromUser",
                    expression = "java(message.getSender().getUsername())"),
            @Mapping(
                    target = "toUser",
                    expression = "java(message.getRecipient().getUsername())"),
    })
    MessageVO message2MessageVO(Message message);

    @AfterMapping
    default void completeConversationVO(@MappingTarget MessageVO messageVO, Message message){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        if(username.equals(message.getSender().getUsername()))
            messageVO.setIsSender(true);
        // 先简单写一下messageVO的 messageBody
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("content", message.getContent());
        messageVO.setMessageBody(messageBody);
    }
}
