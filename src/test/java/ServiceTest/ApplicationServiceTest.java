package ServiceTest;

import aizoo.domain.VisualContainer;
import aizoo.repository.VisualContainerDAO;
import aizoo.service.ApplicationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ApplicationServiceTest {
    @Autowired
    ApplicationService applicationService;

    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Test
    public void renderVisualContainerTest(){
        List<VisualContainer> containers = visualContainerDAO.findAll();
        String resultPath = "C:\\data\\aizoo-slurm\\project\\aizoo\\aizoo-back-interpreter\\files\\gq\\app_result\\124\\test.json";
    }
}
