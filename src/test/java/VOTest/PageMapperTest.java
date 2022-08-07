package VOTest;

import TestBase.SpringbootTestBase;
import aizoo.domain.Component;
import aizoo.domain.Datasource;
import aizoo.repository.ComponentDAO;
import aizoo.repository.DatasourceDAO;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;

import aizoo.viewObject.object.DatasourceVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageMapperTest extends SpringbootTestBase {
    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private DatasourceDAO datasourceDAO;

    @Test
    public void pageMapperTest() {
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "updateTime");
        Page<Component> page = componentDAO.findByComposedAndForkFromIsNull(true, pageable);
        ComponentVOEntityMapper mapper = ComponentVOEntityMapper.MAPPER;
        Page<ComponentVO> newPage = (Page<ComponentVO>) VO2EntityMapper.mapEntityPage2VOPage(mapper::component2ComponentVO, page);
    }

    @Test
    public void pageMapperTest2(){
        Pageable pageable = PageRequest.of(0,10,Sort.Direction.DESC,"updateTime");
        Page<Datasource> datasourcePage = datasourceDAO.findByUserUsername("super", pageable);
        Page<DatasourceVO> newPage = VO2EntityMapper.mapEntityPage2VOPage(DatasourceVOEntityMapper.MAPPER::datasource2DatasourceVO, datasourcePage);
    }

}
