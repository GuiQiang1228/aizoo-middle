package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.Component;
import aizoo.domain.Model;
import aizoo.repository.ModelDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ModelService;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.mapper.ModelVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.ModelVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@BaseResponse
@RestController
public class ModelController {
    @Autowired
    ModelDAO modelDAO;

    @Autowired
    ComponentController componentController;

    @Autowired
    ModelService modelService;

    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/model/getAll", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页模板搜索")
    public Page<ModelVO> getModelPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        //获取页面信息
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //获取所有模板
        Page<Model> modelPage = modelDAO.findAll(pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ModelVOEntityMapper.MAPPER::model2ModelVO, modelPage);
    }

    /**
     * 对已有模板进行加载，并在加载后把图变为未发布状态
     *
     * @param sourceId    service的id
     * @param description
     * @param type        类别
     * @param principal   包含用户信息的对象
     * @return 结果信息，包括成功或失败信息以及相应type的实体
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/model/fork", method = RequestMethod.POST)
    @WebLog(description = "模板fork接口")
    public ResponseResult modelFork(@MultiRequestBody("sourceId") long sourceId,
                                    @MultiRequestBody("description") String description,
                                    @MultiRequestBody("type") String type,
                                    Principal principal) {
        //对所需模板进行fork
        ResponseResult responseResult = componentController.allTypeFork(sourceId, description, type, principal);
        //若fork成功，将图改为未发布状态
        if (responseResult.getMsg().equals("success")) {
            modelService.modifyReleased(responseResult, type);
        }
        return responseResult;
    }
}
