package DAOTest;

import TestBase.SpringbootTestBase;
import aizoo.common.ComponentType;
import aizoo.common.GraphType;
import aizoo.repository.GraphDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class GraphDAOSpringbootTest extends SpringbootTestBase {
    @Autowired
    private GraphDAO graphDAO;

    @Test
    public void findByGraphKeyTest() {
        assert graphDAO.findByGraphKey("MODEL-7d55340e-e1c3-4bd2-b661-b3c0bf68539a") != null;
    }

    @Test
    public void findByUserUsernameAndGraphTypeTest() {
        assert graphDAO.findByUserUsernameAndGraphType("super", GraphType.COMPONENT) != null;
    }

    @Test
    public void findAllByUserUsernameAndGraphTypeTest() {
        Pageable pageable = PageRequest.of(0,10, Sort.Direction.DESC,"updateTime");
        assert graphDAO.findAllByUserUsernameAndGraphType("super", GraphType.COMPONENT,pageable) != null;
    }

    @Test
    public void findByNameAndUserUsernameTest() {
        assert graphDAO.findByNameAndUserUsername("Bert1", "super") != null;
    }

    @Test
    public void findByComponentComponentTypeAndUserUsernameTest() {
        Pageable pageable = PageRequest.of(0,10, Sort.Direction.DESC,"updateTime");
        assert graphDAO.findByComponentComponentTypeAndUserUsername(ComponentType.MODULE, "super",pageable) != null;
    }

    @Test
    public void findByJobsIdTest() {
        assert graphDAO.findByExperimentJobsId((long) 152) == null;
    }
}
