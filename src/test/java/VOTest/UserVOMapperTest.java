package VOTest;

import TestBase.MockitoTestBase;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import aizoo.repository.DatatypeDAO;
import aizoo.repository.UserDAO;
import aizoo.viewObject.object.UserVO;
import aizoo.viewObject.mapper.UserVOEntityMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

public class UserVOMapperTest extends MockitoTestBase {

    private UserVO userVO = new UserVO();
    private List<UserRoleType> roles = new ArrayList<>();
    @MockBean
    private UserDAO userDAO;

    @Before
    public void beforeTest() {
        userVO.setUsername("test");
        roles.add(UserRoleType.USER);
        userVO.setRoles(roles);
    }

    @Test
    public void test(){
        User user = UserVOEntityMapper.MAPPER.userVO2User(userVO,userDAO);
        System.out.println("test userVO2User:"+user.getUsername()+";"+user.getRoles().get(0).getUserRoleType().getValue());

        UserVO uvo = UserVOEntityMapper.MAPPER.userEntity2UserVO(user);
        System.out.println("test user2UserVO:"+uvo.getUsername()+";"+uvo.getRoles().get(0).getValue());
    }
}
