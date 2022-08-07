package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.ModelCategory;
import aizoo.domain.ModelInfo;
import aizoo.repository.GraphDAO;
import aizoo.repository.ModelCategoryDAO;
import aizoo.repository.ModelInfoDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ModelInfoService;
import aizoo.viewObject.mapper.ModelInfoVOEntityMapper;
import aizoo.viewObject.object.ModelInfoVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@BaseResponse
@RestController
public class ModelInfoController {
    @Autowired
    ModelCategoryDAO modelCategoryDAO;

    @Autowired
    ModelInfoDAO modelInfoDAO;

    @Autowired
    ComponentController componentController;

    @Autowired
    ModelInfoService modelInfoService;

    /**
     * 获取模型库目录列表
     *
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/modelCategory/getAll", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取所有目录")
    public List<ModelCategory> getModelCategory() {
        List<ModelCategory> modelCategories = modelCategoryDAO.findAll();
        return modelCategories;
    }

    /**
     * 模型信息搜索
     *
     * @param categoryId        模型目录的id，按此类别返回数据
     * @param pageNum           当前页
     * @param pageSize          每页显示的条数
     * @return Page<ModelInfoVO>类型
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/modelInfo/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "模型信息搜索")
    public Page<ModelInfoVO> searchModelInfoPage(@RequestParam(value = "categoryId", required = false, defaultValue = "") long categoryId,
                                                 @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize) {

        //根据当前页和页面大小，获得分页pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        //根据目录分类别返回模型信息
        Page<ModelInfo> modelInfos = modelInfoDAO.findByModelCategoryId(categoryId, pageable);

        //将modelInfo分页转换成modelInfoVO分页，返回结果
        return VO2EntityMapper.mapEntityPage2VOPage(ModelInfoVOEntityMapper.MAPPER::modelInfo2ModelInfoVO, modelInfos);
    }

    /**
     * 获取对type的fork信息
     * @param modelInfoId 待fork模型的id
     * @param principal 包含用户信息的对象
     * @return 结果信息，成功fork:返回跳转的url， fork失败：返回失败信息
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/model/allTypeFork", method = RequestMethod.POST)
    @WebLog(description = "模型库fork")
    public ResponseResult allTypeFork(@MultiRequestBody("modelInfoId") long modelInfoId,
                                      Principal principal) {
        ResponseResult responseResult = modelInfoService.forkModel(modelInfoId, principal);
        if(responseResult.getMsg().equals("success")){
            String url = modelInfoService.getUrl(modelInfoId, responseResult);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", url);
        }
        return responseResult;
    }

    /**
     * 模型总览页面按类别返回各模型的总数以及top5
     *
     * @return HashMap<String, HashMap<String, Object>>，七种模型的每种的总数以及top5的VO列表
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/modelInfo/getAllByType", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "模型总览")
    public ResponseResult getAllByType() {
        HashMap<String, HashMap<String, Object>> infoMap = modelInfoService.getAllInfo();
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), "success", infoMap);
    }

}
