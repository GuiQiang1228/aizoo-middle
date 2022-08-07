package aizoo.controller;

import aizoo.aspect.WebLog;
import aizoo.domain.Datatype;
import aizoo.repository.DatatypeDAO;
import aizoo.response.BaseResponse;
import aizoo.viewObject.mapper.DatatypeVOEntityMapper;
import aizoo.viewObject.object.DatatypeVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@BaseResponse
@RestController
public class DatatypeController {
    @Autowired
    DatatypeDAO datatypeDAO;

    /**
     * 获取数据类型列表
     * @return List类型
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/getDatatype",method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取数据类型列表")
    public List<DatatypeVO> getAllDatatype(){
        // 1、根据数据类型获取数据组成list
        List<Datatype> datatypeList = datatypeDAO.findAll();
        // 2、返回结果
        return VO2EntityMapper.mapEntityList2VOList(DatatypeVOEntityMapper.MAPPER::Datatype2DatatypeVO,datatypeList);
    }
}
