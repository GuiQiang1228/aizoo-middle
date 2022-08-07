package VOTest;

import TestBase.MockitoTestBase;
import aizoo.domain.Namespace;
import aizoo.domain.Role;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import aizoo.viewObject.object.NamespaceVO;
import aizoo.viewObject.mapper.NamespaceVOEntityMapper;
import org.junit.Test;

import java.util.Arrays;


public class NamespaceVOMapperTest extends MockitoTestBase {


    @Test
    public void test(){
        Namespace namespace = new Namespace("super.module.add");
        namespace.setPrivacy("public");
        User user = new User("super");
        Role role = new Role();
        role.setUserRoleType(UserRoleType.USER);
        user.setRoles(Arrays.asList(role));
        namespace.setUser(user);
        NamespaceVO namespaceVO = NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(namespace);
        System.out.println(namespaceVO);

        Namespace namespace1 = NamespaceVOEntityMapper.MAPPER.namespaceVO2Namespace(namespaceVO);
        System.out.println(namespace1);
    }
}
