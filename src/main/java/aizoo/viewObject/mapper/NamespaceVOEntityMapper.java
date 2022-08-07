package aizoo.viewObject.mapper;

import aizoo.domain.Namespace;
import aizoo.repository.NamespaceDAO;
import aizoo.viewObject.object.NamespaceVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NamespaceVOEntityMapper {

    NamespaceVOEntityMapper MAPPER = Mappers.getMapper(NamespaceVOEntityMapper.class);
    @Mappings({
            @Mapping(target = "userVO", expression = "java(UserVOEntityMapper.MAPPER.userEntity2UserVO(namespace.getUser()))"),
            @Mapping(target = "namespace", expression = "java(namespace.getNamespace())"),
            @Mapping(target = "username", expression = "java(namespace.getUser().getUsername())")}
    )
    NamespaceVO namespace2NamespaceVO(Namespace namespace);

    @Mapping(target = "namespace",qualifiedByName = "namespace2Namespace")
    Namespace namespaceVO2Namespace(NamespaceVO namespaceVO);

    @Named("namespace2Namespace")
    default Namespace namespace2Namespace(String namespace,@Context NamespaceDAO namespaceDAO){
        return namespaceDAO.findByNamespace(namespace);
    }
}
