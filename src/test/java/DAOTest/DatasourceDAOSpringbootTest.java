package DAOTest;

import TestBase.SpringbootTestBase;
import aizoo.domain.Datasource;
import aizoo.domain.Namespace;
import aizoo.domain.User;
import aizoo.repository.DatasourceDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.ArrayList;
import java.util.List;


public class DatasourceDAOSpringbootTest extends SpringbootTestBase {
    @MockBean
    private DatasourceDAO datasourceDAO;

    @Autowired
    private DatasourceDAO datasourceDAO1;

    @Before
    public void beforeTest(){
        User user = new User();
        user.setUsername("test");
        User user1 = new User();
        user1.setUsername("super");
        Namespace namespace = new Namespace();
        namespace.setNamespace("test.namespace");
        Namespace namespace1 = new Namespace();
        namespace1.setNamespace("super.namespace");
        Datasource datasource = new Datasource(1L,"private", "d", "this is a datasource", "description", "aizoo/user", user, namespace);
        Datasource datasource1 = new Datasource(2L,"private", "d1", "this is a datasource1", "description1", "aizoo/user", user1, namespace1);
        List<Datasource> list = new ArrayList<>();
        list.add(datasource);
        list.add(datasource1);
        List<Datasource> list1 = new ArrayList<>();
        list1.add(datasource);
        List<Datasource> list2 = new ArrayList<>();
        list2.add(datasource1);
        Mockito.when(datasourceDAO.getOne(1L)).thenReturn(datasource);
        Mockito.when(datasourceDAO.getOne(2L)).thenReturn(datasource1);
        Mockito.when(datasourceDAO.findByName("test.datasource")).thenReturn(datasource);
    }

}