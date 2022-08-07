package aizoo.service;

import aizoo.domain.Component;
import aizoo.domain.Graph;
import aizoo.repository.ComponentDAO;
import aizoo.repository.GraphDAO;
import aizoo.response.ResponseResult;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.GraphVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.nio.file.Paths;
import java.util.Locale;

@Service("ModelService")
public class ModelService {
    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    GraphDAO graphDAO;

    public void modifyReleased(ResponseResult responseResult, String type) {
        Graph graph = null;
        if (type.equals("COMPONENT")) {
            ComponentVO componentVO = (ComponentVO) responseResult.getData();
            Component component = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
            if (component.isComposed())
                graph = component.getGraph();
        } else if (type.equals("SERVICE")) {
            aizoo.domain.Service service = (aizoo.domain.Service) responseResult.getData();
            graph = service.getGraph();
        } else {
            GraphVO graphVO = (GraphVO) responseResult.getData();
            graph = graphDAO.findById(graphVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(graphVO.getId())));
        }
        if (graph != null) {
            graph.setReleased(false);
            graphDAO.save(graph);
        }
    }
}
