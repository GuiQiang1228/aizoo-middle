package ServiceTest;

import TestBase.SpringbootTestBase;
import aizoo.repository.GraphDAO;
import aizoo.service.GraphService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class GraphServiceTest extends SpringbootTestBase {
    @Autowired
    GraphService graphService;
    @Autowired
    GraphDAO graphDAO;

//    @Test
//    public void validGraphNameTest(){
//        System.out.println(graphService.validGraphName("Bert1","MODULE","super"));
//    }

//    @Test
//    public void getGraphListTest(){
//        System.out.println(graphService.getGraphList(0,10,"super","MODEL"));
//    }
/*
    @Test
    public void createGraphTest(){
        System.out.println(graphService.createGraph("Bert11","MODULE","super.module.bert1",  "super", "public"));
    }
*/
//    @Test
//    @Transactional
//    public void openGraphTest(){
//        System.out.println(graphService.openGraph((long) 25));
//    }
//
//    @Test
//    public void deleteGraphTest() throws Exception {
//        graphService.deleteGraph(134,"super");
//    }

}
