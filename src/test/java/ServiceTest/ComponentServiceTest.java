package ServiceTest;

import TestBase.SpringbootTestBase;
import aizoo.repository.ComponentDAO;
import aizoo.service.ComponentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class ComponentServiceTest extends SpringbootTestBase {
    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    ComponentService componentService;

    @Test
    public void getComponentByKeywordTest(){
        System.out.println(componentService.getComponentByKeyword("private","super","MODULE","Ber"));
    }

    @Test
    public void getComponentByType(){
        System.out.println(componentService.getComponentByType("super","private","MODULE"));
    }

    /*@Test
    public void forkComponent() throws Exception {
        System.out.println(componentService.forkComponent("super",(long)1,"super1.model.bert","private","fork的bert组件"));
    }*/

    @Test
    public void deleteUploadComponent() throws Exception {
        componentService.deleteUploadComponent((long)1);
    }
}
