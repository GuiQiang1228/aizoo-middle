package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.CheckPoint;
import aizoo.repository.CheckPointDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.CodeService;
import aizoo.service.ResourceUsageService;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.CheckPointVOEntityMapper;
import aizoo.viewObject.object.CheckPointVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import aizoo.service.CheckPointService;
import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.security.Principal;
import java.util.List;

@BaseResponse
@RestController
public class CheckPointController {

    @Autowired
    CheckPointDAO checkPointDAO;


    @Autowired
    CheckPointService checkPointService;

    @Autowired
    private ResourceUsageService resourceUsageService;
    //private static final Logger logger = LoggerFactory.getLogger(MirrorJobController.class);

    /**
     * 利用springboot自带分页功能进行分页搜索
     * 参数为搜索需要的信息
     * @param name CheckPoint名称
     * @param desc CheckPoint描述
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime 结束更新时间
     * @param pageNum 当前页号
     * @param pageSize 每页有几条记录
     * @param principal 用户信息
     * @return 返回查询到的转换格式后的Page对象
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/CheckPoint/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页CheckPoint搜索")
    public Page<CheckPointVO> searchPage(@RequestParam(value = "name", required = false, defaultValue = "") String name,
                                         @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                         @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                         @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        //不带排序的pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //加入了pageable对象的查询语句，通过该pageable对象分析生成一个带分页查询的sql语句，返回存储JPA查询数据库的结果集（checkpointsPage)
        Page<CheckPoint> checkpointsPage = checkPointDAO.searchCheckPoint(name, desc, startUpdateTime, endUpdateTime, userName, pageable);
        //利用jpa中的page.map方法转换checkpointsPage的内部对象(转换为CheckPointVO)
        //第一个参数利用双冒号::简化方法引用，实际是调用CheckPoint2CheckPointVO方法
        return VO2EntityMapper.mapEntityPage2VOPage(CheckPointVOEntityMapper.MAPPER::CheckPoint2CheckPointVO, checkpointsPage);
    }

    /**
     * 删除资源管理页面用户拥有的CheckPoint资源
     *
     * @param id        CheckPoint的id
     * @param principal 用户信息
     * @return 删除信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/deleteCheckPoint", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除资源管理页面用户拥有的CheckPoint资源")
    public ResponseResult deleteCheckPoint(@MultiRequestBody("id") long id, Principal principal) throws Exception {
        //根据id查找CheckPoint
        CheckPoint checkPoint = checkPointDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        //判断用户名是否相等
        if (!checkPoint.getUser().getUsername().equals(principal.getName())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        //判断是否被其他进程占用
        File file = new File(checkPoint.getPath());
        //保留原来路径以及重命名所需的测试路径
        String initPath = file.getPath();
        String temPath = file.getPath().replace(file.getName(), "test.avi");
        //如果可以重命名，证明可以删除
        if(file.renameTo(new File(temPath))){
            //删除checkpoint
            new File(temPath).renameTo(new File(initPath));
            checkPointService.deleteCheckPoint(id);
            resourceUsageService.updateDiskCapacity(principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
        }
        else
            return new ResponseResult(ResponseCode.CHECKPOINT_DELETE_ERROR.getCode(), ResponseCode.CHECKPOINT_DELETE_ERROR.getMsg(), null);
    }
}
