package aizoo.service;

import aizoo.controller.ComponentController;
import aizoo.domain.Component;
import aizoo.domain.Graph;
import aizoo.domain.ModelCategory;
import aizoo.domain.ModelInfo;
import aizoo.repository.ComponentDAO;
import aizoo.repository.GraphDAO;
import aizoo.repository.ModelCategoryDAO;
import aizoo.repository.ModelInfoDAO;
import aizoo.response.ResponseResult;
import aizoo.viewObject.mapper.ModelInfoVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.ModelInfoVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service("ModelInfoService")
public class ModelInfoService {
    @Autowired
    ModelInfoDAO modelInfoDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Value("${file.path}")
    String filePath;

    @Autowired
    ComponentController componentController;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    ModelCategoryDAO modelCategoryDAO;

    /**
     * 模型库fork后获得跳转url
     *
     * @param modelInfoId    待fork模型的id
     * @param responseResult 执行fork时的返回结果
     * @return
     */
    public String getUrl(long modelInfoId, ResponseResult responseResult) {
        ModelInfo modelInfo = modelInfoDAO.findById(modelInfoId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(modelInfoId)));
        String url = "";
        String type = "";
        //如果模型类型是算子，组织返回给前端的url为：notebook/edit/component.path(相对路径)
        if (!modelInfo.isGraph()) {
            ComponentVO componentVO = (ComponentVO) responseResult.getData();
            Component component = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
            url = Paths.get("notebook", "edit", component.getPath().replaceAll(filePath, "")).toString();
        } else { //如果类型为图，由于原fork方法对于不同值的返回数据不一样，所以需要分别组织，返回的url形式为 /design/graphId
            type = modelInfo.getGraphType().getName().toUpperCase(Locale.ROOT);
            if (type.equals("COMPONENT")) {
                ComponentVO componentVO = (ComponentVO) responseResult.getData();
                Component component = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
                url = Paths.get("design", component.getGraph().getId().toString()).toString();
            } else if (type.equals("SERVICE")) {
                aizoo.domain.Service service = (aizoo.domain.Service) responseResult.getData();
                Graph graph = service.getGraph();
                url = Paths.get("design", graph.getId().toString()).toString();
            } else {
                GraphVO graphVO = (GraphVO) responseResult.getData();
                url = Paths.get("design", graphVO.getId().toString()).toString();
            }
        }
        return url;
    }

    /**
     * fork模型库中的模型
     *
     * @param modelInfoId 待fork模型的id
     * @param principal
     * @return
     */
    public ResponseResult forkModel(long modelInfoId, Principal principal) {
        ModelInfo modelInfo = modelInfoDAO.findById(modelInfoId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(modelInfoId)));
        String type = "";
        long sourceId = modelInfo.getSourceId();
        //对于不同类型的fork分别拿到其对应类型的id和type值，并进行fork
        if (modelInfo.isGraph()) {
            type = modelInfo.getGraphType().getName().toUpperCase(Locale.ROOT);
            Graph graph = graphDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(modelInfo.getSourceId())));
            if (type.equals("SERVICE")) {
                sourceId = graph.getService().getId();
            } else if (type.equals("COMPONENT")) {
                sourceId = graph.getComponent().getId();
            } else if (type.equals("JOB"))
                type = "EXPERIMENT";
        } else
            type = "COMPONENT";
        ResponseResult responseResult = componentController.allTypeFork(sourceId, "", type, principal);
        return responseResult;
    }

    /**
     *按类别返回各模型的总数以及top5
     *
     * @return HashMap<String, HashMap<String, Object>>，七种模型的每种的总数以及top5的VO列表
     */
    public HashMap<String, HashMap<String, Object>> getAllInfo() {
        HashMap<String, HashMap<String, Object>> infoMap = new HashMap<>();
        //获取所有的模型目录
        List<ModelCategory> modelCategories = modelCategoryDAO.findAll();
        for(ModelCategory modelCategory : modelCategories){
            //按每种模型目录搜索对应的所有模型
            List<ModelInfo> modelInfos = modelInfoDAO.findByModelCategoryId(modelCategory.getId());
            //将搜索到的模型列表转为VO列表
            List<ModelInfoVO> modelInfoVOS = VO2EntityMapper.mapEntityList2VOList(ModelInfoVOEntityMapper.MAPPER::modelInfo2ModelInfoVO, modelInfos);
            //取其top5以后将top5和总数以及模型目录本身的各种信息放入map
            modelInfoVOS = modelInfoVOS.subList(0, 5);
            HashMap<String, Object> contentMap = new HashMap<>();
            contentMap.put("id", modelCategory.getId());
            contentMap.put("num", modelInfos.size());
            contentMap.put("infoList", modelInfoVOS);
            contentMap.put("SceneIntroduction", modelCategory.getSceneIntroduction());
            contentMap.put("scenePicture", modelCategory.getScenePicture());
            contentMap.put("icon", modelCategory.getIcon());
            infoMap.put(modelCategory.getName(), contentMap);
        }
        return infoMap;
    }
}
