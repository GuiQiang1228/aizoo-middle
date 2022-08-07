package aizoo.viewObject.mapper;

import aizoo.domain.Application;
import aizoo.domain.Code;
import aizoo.repository.UserDAO;
import aizoo.viewObject.object.ApplicationVO;
import aizoo.viewObject.object.CodeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CodeVOEntityMapper {
    CodeVOEntityMapper MAPPER = Mappers.getMapper(CodeVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    target = "username",
                    expression = "java(code.getUser().getUsername())"
            ),
            @Mapping(
                    target = "path",
                    ignore = true
            )
    })
    CodeVO code2CodeVO(Code code);

    @Mappings({
            @Mapping(
                    target = "user",
                    expression = "java(userDAO.findByUsername(codeVO.getUsername()))")
    })
    Code codeVO2Code(CodeVO codeVO, @Context UserDAO userDAO);
}
