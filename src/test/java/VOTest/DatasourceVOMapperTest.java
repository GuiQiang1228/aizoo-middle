package VOTest;

import TestBase.MockitoTestBase;
import aizoo.domain.Datasource;
import aizoo.domain.Namespace;
import aizoo.domain.Role;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import aizoo.viewObject.object.DatasourceVO;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import org.junit.Test;

import java.util.Arrays;

public class DatasourceVOMapperTest extends MockitoTestBase {

    @Test
    public void mapperTest(){
        Datasource datasource = new Datasource();
        datasource.setName("d1");
        datasource.setDescription("数据资源");
        datasource.setPrivacy("private");
        datasource.setDescription("this is a datasource");

        User user = new User("super");
        Role role = new Role();
        role.setUserRoleType(UserRoleType.USER);
        user.setRoles(Arrays.asList(role));
        Namespace namespace = new Namespace("super.datasource.d1");
        namespace.setUser(user);
        datasource.setNamespace(namespace);
        datasource.setUser(user);

        DatasourceVO datasourceVO = DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(datasource);
        System.out.println(datasourceVO);
    }

}
