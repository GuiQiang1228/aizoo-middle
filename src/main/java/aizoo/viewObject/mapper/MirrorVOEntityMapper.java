package aizoo.viewObject.mapper;

import aizoo.domain.Component;
import aizoo.domain.Mirror;
import aizoo.domain.User;
import aizoo.repository.UserDAO;
import aizoo.viewObject.object.MirrorVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MirrorVOEntityMapper {
    MirrorVOEntityMapper MAPPER = Mappers.getMapper(MirrorVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    target = "userName",
                    expression = "java(mirror.getUser().getUsername())"
            ),
            @Mapping(
                    target = "path",
                    ignore = true
            ),
    })
    MirrorVO mirror2MirrorVO(Mirror mirror);

    @Mappings({
            @Mapping(
                    target = "user",
                    expression = "java(userDAO.findByUsername(mirrorVO.getUserName()))"
            ),
    })
    Mirror mirrorVO2Mirror(MirrorVO mirrorVO, @Context UserDAO userDAO);

    @AfterMapping
    default void completeMirror(MirrorVO mirrorVO, @MappingTarget Mirror mirror, @Context UserDAO userDAO) {
        if(mirrorVO.getUserName() != null){
            User user = userDAO.findByUsername(mirrorVO.getUserName());
            mirror.setUser(user);
        }
    }

    @AfterMapping
    default void completeMirrorVO(Mirror mirror, @MappingTarget MirrorVO mirrorVO) {
        if(mirror.getUser() != null)
            mirrorVO.setUserName(mirror.getUser().getUsername());
    }
}
