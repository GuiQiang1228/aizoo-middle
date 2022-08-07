package aizoo.controller;

import aizoo.aspect.WebLog;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;


@BaseResponse
@RestController
public class CommonController {

    @Autowired
    CommonService commonService;

    /**
     * 检查所有entity的关联情况
     *
     * @param id
     * @param type      (可传值为：component service experimentJob  serviceJob graph project datasource)
     * @return ResponseResult类型
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/common/check/relation", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "检查所有entity的关联情况")
    public ResponseResult checkCommon(@RequestParam(value = "id") long id,
                                      @RequestParam(value = "type") String type) {
        //检查所有entity的关联情况
        HashMap<String, HashMap<Long, Object>> object = commonService.checkCommon(id, type);
        //返回结果
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), object);
    }
}
