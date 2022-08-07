package DAOTest;

import TestBase.SpringbootTestBase;
import aizoo.domain.Component;
import aizoo.common.ComponentType;
import aizoo.repository.ComponentDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ComponentDAOSpringbootTest extends SpringbootTestBase {
    @Autowired
    private ComponentDAO componentDAO;


    @Test
    public void findByComponentTypeAndUserUsernameAndPathIsNotNullTest() {
        assert componentDAO.findByComponentTypeAndUserUsernameAndPathIsNotNull(ComponentType.MODULE,"super")!= null;
    }

    @Test
    public void findByComponentTypeAndUserUsernameNotAndPrivacyAndPathIsNotNullTest() {
        ComponentType type = ComponentType.MODULE;
        assert componentDAO.findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(type,"private","super1","Ber") != null;
    }

    @Test
    public void findByUserUsernameAndComposedAndForkFromIsNullTest() {
        String username = "super";
        Pageable pageable = PageRequest.of(0,10, Sort.Direction.DESC,"updateTime");
        assert componentDAO.findByUserUsernameAndComposed(username,false,pageable)!= null;
    }

    @Test
    public void findByComposedTest() {
        Pageable pageable = PageRequest.of(0,10, Sort.Direction.DESC,"updateTime");
        assert componentDAO.findByComposedAndForkFromIsNull(false,pageable)!= null;
    }


    @Test
    public void findByUserUsernameTest() {
        assert componentDAO.findByUserUsername("super")!= null;
    }

    @Test
    public void findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNullTest() {
        String like = "Ber";
        assert componentDAO.findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(ComponentType.MODULE,"private","super",like)!= null;
    }

    @Test
    public void findByComponentTypeAndUserUsernameAndTitleLikeAndPathIsNotNullTest() {
        assert componentDAO.findByComponentTypeAndUserUsernameAndTitleLikeAndPathIsNotNull(ComponentType.MODULE,"super","Ber")!= null;
    }

    @Test
    public void existsByForkFromAndUserUsernameTest() {
        Component component = componentDAO.getOne((long)1);
        assert !componentDAO.existsByForkFromAndUserUsername(component, "super1");
    }

    @Test
    public void findByPrivacyTest() {
        assert componentDAO.findByPrivacy("private")!= null;
    }

    @Test
    public void existsByNamespaceNamespaceTest() {
        assert componentDAO.existsByNamespaceNamespace("super.module.bert");
    }

}