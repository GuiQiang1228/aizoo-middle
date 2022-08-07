package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.domain.Datasource;
import aizoo.repository.DatasourceDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.DatasourceService;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import aizoo.viewObject.object.DatasourceVO;
import aizoo.viewObject.object.TFileInfoVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;

import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;

@BaseResponse
@RestController
public class DatasourceController {

    @Autowired
    private DatasourceDAO datasourceDAO;

    @Autowired
    private DatasourceService datasourceService;

    /**
     * 根据用户名和数据资源的私有/公开权限展示从数据库中取到的list
     * @param privacy 目前项目中privacy的值为private/public，参数由前端传递
     * @param principal 该参数无需前端传递，加上不会影响前端定义的接口参数，principal存储了当前登录用户的相关信息，比如username
     * @return 将数据资源类型的Vo以List的方式传给前端
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/datasource",method= RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "画图页面的左侧目录 获取数据资源列表")
    public List<DatasourceVO> getDatasource(@RequestParam String privacy, Principal principal) {
        // 调用datasourceService文件中的getDataSource方法，将返回的DataSource转成Vo
        return ListEntity2ListVO.datasource2DatasourceVO(
                datasourceService.getDatasource(principal.getName(),privacy));
    }

    /**
     * 根据搜索输入框中的关键字从数据库中查找数据资源，搜索方式查找的数据资源不仅包含用户自己的，还有其他用户或者系统公开的数据资源
     * @param privacy private/public
     * @param keyword 用户输入的搜索关键字
     * @param principal 从用户的登录信息中取username
     * @return 将数据资源类型的Vo以List的方式传给前端
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/searchDatasource",method= RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "画图页面的左侧目录 根据关键字搜索数据资源")
    public List<DatasourceVO> getDataBYKeyword(@RequestParam String privacy, @RequestParam String keyword,
                                               Principal principal){
        return ListEntity2ListVO.datasource2DatasourceVO(
                datasourceService.getDatasourceByKeyword(principal.getName(),privacy,keyword));
    }

    /**
     * 上传管理页面，根据搜索信息来查询数据库中对应的数据资源信息接口
     * @param namespace DataSource存在于什么命名空间
     * @param privacy   DataSource的私有/公开属性
     * @param desc      DataSource的描述信息
     * @param startUpdateTime
     * @param endUpdateTime    通过start和end两个时间来查找改时间段上传的数据资源
     * @param name      DataSource名字
     * @param pageNum   表示数据分页后的第几页，默认值为0
     * @param pageSize  数据分页每一页的大小，默认值为10
     * @param principal
     * @return          按照分页要求分好页的DataSource信息
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/resource/datasource/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页datasource搜索")
    public Page<DatasourceVO> searchDatasourcePage(@RequestParam(value = "namespace", required = false, defaultValue = "") String namespace,
                                                   @RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                                   @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                                   @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                                   @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                                   @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                   @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();

        // 根据pageNum和pageSize的格式返回给前端展示数据
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // 通过前端传过来的参数查询数据库并返回结果
        Page<Datasource> namespacePage = datasourceDAO.searchDatasource(namespace, privacy, desc, startUpdateTime, endUpdateTime, name, userName, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(DatasourceVOEntityMapper.MAPPER::datasource2DatasourceVO, namespacePage);
    }

    /**
     * 修改上传管理页面用户拥有的数据资源的描述信息，也包括了输出参数的描述
     * @param datasourceVO 前端将用户修改后的描述信息和信息打包成VO格式传给后端
     * @return 返回的是接口成功调用的相关信息
     * @throws JsonProcessingException
     * @throws URISyntaxException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/changeDatasource", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改上传管理页面用户拥有的数据资源的描述信息")
    public ResponseResult changeDatasource(@MultiRequestBody("datasource") DatasourceVO datasourceVO ) throws JsonProcessingException, URISyntaxException {
        datasourceService.updateDesc(datasourceVO);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 云上传数据资源
     *
     * @param datasourceVO 数据资源
     * @return 上传数据资源信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/Datasource", method = RequestMethod.POST)
    @WebLog(description = "云上传数据资源")
    public ResponseResult uploadDatasource(@MultiRequestBody("datasource") DatasourceVO datasourceVO) {
        try {
            datasourceService.uploadDatasource(datasourceVO);
        } catch (Exception e) {
            return new ResponseResult(ResponseCode.DATASOURCE_UPLOAD_ERROR.getCode(),
                    ResponseCode.DATASOURCE_UPLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMsg(), null);
    }

}
