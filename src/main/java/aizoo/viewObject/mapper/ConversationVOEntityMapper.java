package aizoo.viewObject.mapper;

import aizoo.domain.Conversation;
import aizoo.domain.Message;
import aizoo.repository.MessageDAO;
import aizoo.viewObject.object.ConversationVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;

@Mapper(componentModel = "spring")
public interface ConversationVOEntityMapper {

    ConversationVOEntityMapper MAPPER = Mappers.getMapper(ConversationVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    target = "participant",
                    expression = "java(conversation.getParticipant().getUsername())")
    })
    ConversationVO conversation2ConversationVO(Conversation conversation, @Context MessageDAO messageDAO);

    @AfterMapping
    default void completeConversationVO(@MappingTarget ConversationVO conversationVO, Conversation conversation, @Context MessageDAO messageDAO){
        if(conversation.getLatestMessage() != null){
            Message message = messageDAO.findById(conversation.getLatestMessage()).orElseThrow(() -> new EntityNotFoundException(""));
            conversationVO.setMessageType(message.getMessageType());
            switch (message.getMessageType()){
                case TEXT: conversationVO.setContent(message.getContent()); break;
                case SHARE: conversationVO.setContent(message.getContent()); break;
                case GRAPH: conversationVO.setContent("[图片]"); break;
            }
        }
        conversationVO.setEmail(conversation.getParticipant().getEmail());
    }
}
