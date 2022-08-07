package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.response.ResponseResult;
import aizoo.service.DockerService;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.ExperimentJobVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.security.Principal;

@Controller
public class DockerController {
    @Autowired
    DockerService dockerService;

    @RequestMapping(value = "/api/job/downDockerPackage", method = RequestMethod.GET)
    @WebLog(description = "下载job的docker包")
    public ResponseResult downloadDockerPackage(@MultiRequestBody("graph") GraphVO graphVO, @MultiRequestBody("job") ExperimentJobVO experimentJobVO, Principal principal) throws Exception {
        String username = principal.getName();
//        dockerService.downDockerPackage(graphVO, experimentJobVO,username);
        return null;
    }
}
