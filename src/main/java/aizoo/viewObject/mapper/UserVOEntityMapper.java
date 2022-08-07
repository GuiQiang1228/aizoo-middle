package aizoo.viewObject.mapper;

import aizoo.common.LevelType;
import aizoo.domain.Level;
import aizoo.domain.Role;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import aizoo.repository.UserDAO;
import aizoo.viewObject.object.UserVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Mapper(componentModel = "spring",nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface UserVOEntityMapper {
    UserVOEntityMapper MAPPER = Mappers.getMapper(UserVOEntityMapper.class);

    User userVO2User(UserVO userVO,@Context UserDAO userDAO);

    @Mappings({
            @Mapping(
                    source = "level",
                    target = "level",
                    qualifiedByName = "level2levelVO"),
            @Mapping(
                    source = "roles",
                    target = "roles",
                    qualifiedByName = "userEntity2UserVORoles")
    })

    UserVO userEntity2UserVO(User user);


    @Named("level2levelVO")
    default LevelType level2levelVO(Level level){
        LevelType l = level.getName();
        return l;
    }


    @Named("userEntity2UserVORoles")
    default List<UserRoleType> userEntity2UserVORoles(List<Role> var){
        List<UserRoleType> list = new ArrayList<>();
        for (Role role:var){
            list.add(role.getUserRoleType());
        }
        return list;
    }





    @AfterMapping
    default void completeUser(@MappingTarget User user, @Context UserDAO userDAO){
        User user1 = userDAO.findByUsername(user.getUsername());
        user.setPassword(user1.getPassword());
        user.setNamespaces(user1.getNamespaces());
        user.setDatatype(user1.getDatatype());
        user.setComponents(user1.getComponents());
        user.setGraphs(user1.getGraphs());
        user.setExperimentJobs(user1.getExperimentJobs());

    }

   /* default void completeUserVO(@MappingTarget UserVO userVO, User user){
        if(user.getLevel()!=null){
            userVO.setLevel(user.getLevel().getName());
        }
    }*/
}
