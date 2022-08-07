package DAOTest;

import TestBase.SpringbootTestBase;
import aizoo.domain.Role;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import aizoo.repository.UserDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

public class UserDAOSpringbootTest extends SpringbootTestBase {
    @MockBean
    private UserDAO userDAO;

    @Autowired
    private UserDAO userDAO1;

    @Before
    public void beforeTest() {
        String name = "test1";
        List<Role> roleList = new ArrayList<>();
        UserRoleType userRoleType=UserRoleType.USER;
        Role role = new Role();
        role.setUserRoleType(userRoleType);
        roleList.add(role);
        User user = new User();
        user.setRoles(roleList);
        user.setUsername(name);

        List<User> userList = new ArrayList<>();
        userList.add(user);

        Mockito.when(userDAO.findByUsername(name)).
                thenReturn(user);

        Mockito.when(userDAO.findAll()).
                thenReturn(userList);

        Mockito.when(userDAO.findByRolesUserRoleType(UserRoleType.valueOf("USER"))).
                thenReturn(userList);
    }

    @Test
    public void findByUsernameTest() {
        String name = "test1";
        System.out.println(userDAO1.findByUsername("test1").getUsername());
//      assert userDAO.findByUsername(name) != null;
        assert userDAO1.findByUsername(name) != null;
    }

    @Test
    public void findAll(){
        assert userDAO1.findAll() != null;
        System.out.println(userDAO1.findAll());
    }


    @Test
    public void findByRoles(){
        String role = "USER";
        assert userDAO1.findByRolesUserRoleType(UserRoleType.valueOf(role)) != null;
        System.out.println(userDAO1.findByRolesUserRoleType(UserRoleType.valueOf(role)));
    }
}
