package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.Project;
import aizoo.repository.ProjectDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ProjectService;
import aizoo.viewObject.mapper.ProjectVOEntityMapper;
import aizoo.viewObject.object.ProjectVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.List;

@BaseResponse
@RestController
public class ProjectController {
    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectDAO projectDAO;

    /**
     * 新建项目的方法
     * @param name  项目名称
     * @param privacy   private/public
     * @param desc  项目描述
     * @param principal   用户信息
     * @return  返回新建成功的project类型的变量
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/create", method = RequestMethod.POST)
    @WebLog(description = "新建项目")
    public Project createProject(@MultiRequestBody String name, @MultiRequestBody String privacy,
                                 @MultiRequestBody(value = "description") String desc, Principal principal) {
        return projectService.createProject(name, principal.getName(), privacy, desc);
    }

    /**
     *根据project的id来删除
     * @param id  前端传递的要删除project的ID信息
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/delete", method = RequestMethod.POST)
    @WebLog(description = "删除项目")
    public void deleteProject(@MultiRequestBody long id) throws Exception {
        projectService.deleteProject(id);
    }

    /**
     *获取项目列表信息，用户点击前一页或者后一页的时候触发
     * @param principal  用户信息
     * @param pageNum   分页后的第几页
     * @param pageSize  分页大小，默认十条数据为一页
     * @return  将project实体结构转为分页好的VO格式发送给前端
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/getList", method = RequestMethod.GET)
    @WebLog(description = "获取项目列表")
    public Page<ProjectVO> getProjectList(Principal principal,
                                          @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");
        // 返回用户想要查看第几页的项目信息
        Page<Project> projectPage = projectDAO.findByUserUsername(principal.getName(), pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ProjectVOEntityMapper.MAPPER::project2ProjectVO, projectPage);
    }

    /**
     * project的搜索方法
     * @param privacy   搜索内容的私有性
     * @param desc      搜索project的描述
     * @param startUpdateTime
     * @param endUpdateTime    start和end两个时间段的内创建的project
     * @param name      搜索project的名字
     * @param pageNum   页号
     * @param pageSize  分页大小
     * @param principal  后台可以自己获取的用户的相关信息结构，包括username等
     * @return          将project实体结构转为分页好的VO格式发送给前端
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/project/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页project搜索")
    public Page<ProjectVO> searchProjectPage(@RequestParam(value = "privacy", required = false, defaultValue = "") String privacy,
                                             @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                             @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                             @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                             @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                             @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                             @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Project> projectPage = projectDAO.searchProject(privacy, desc, startUpdateTime, endUpdateTime, name, userName, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ProjectVOEntityMapper.MAPPER::project2ProjectVO, projectPage);
    }

    /**
     * 修改项目信息方法，可修改项目姓名，私有性，和项目描述
     * @param principal
     * @param id    需要修改信息的projectID
     * @param name  项目改动后的新名字
     * @param privacy   私有性更改
     * @param desc  项目描述信息更改
     * @return  返回修改是否成功的信息，CHANGE_OUT_OF_BOUNDS(10073, "非本人资源，无法修改"),SUCCESS(200, "成功")
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/modify", method = RequestMethod.POST)
    @WebLog(description = "修改项目信息")
    public ResponseResult modifyProject(Principal principal, @MultiRequestBody long id, @MultiRequestBody String name, @MultiRequestBody String privacy,
                                        @MultiRequestBody(value = "description") String desc) {

        // flag标志表示修改操作是否成功
        boolean flag = projectService.modifyProject(principal.getName(), id, name, privacy, desc);
        if (!flag) // 失败返回错误信息
            return new ResponseResult(ResponseCode.CHANGE_OUT_OF_BOUNDS.getCode(), ResponseCode.CHANGE_OUT_OF_BOUNDS.getMsg(), null);

        // 返回成功信息
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 在项目中增删内容
     * @param principal
     * @param projectId   要修改的project ID
     * @param type        增删内容的类型
     * @param contentIdList 具体增删的组件ID
     * @return  返回方法是否成功的消息体，CHANGE_OUT_OF_BOUNDS(10073, "非本人资源，无法修改"),SUCCESS(200, "成功")
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/content/modify", method = RequestMethod.POST)
    @WebLog(description = "在项目中增删内容(可批量增删)")
    public ResponseResult projectAddContent(Principal principal, @MultiRequestBody long projectId, @MultiRequestBody String type, @MultiRequestBody List<Integer> contentIdList) {
        // flag标志表示修改操作是否成功
        boolean flag = projectService.projectModifyContent(principal.getName(), projectId, type, contentIdList);
        if (!flag) // 失败返回错误信息
            return new ResponseResult(ResponseCode.CHANGE_OUT_OF_BOUNDS.getCode(), ResponseCode.CHANGE_OUT_OF_BOUNDS.getMsg(), null);
        // 返回成功信息
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 删除项目中的某些内容
     * @param principal
     * @param projectId     要修改的project ID
     * @param type          删除内容的类型
     * @param contentId     删除的组件ID
     * @return              返回方法是否成功的消息体，DELETE_OUT_OF_BOUNDS(10072,"非本人资源，无法删除"),,SUCCESS(200, "成功")
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/content/delete", method = RequestMethod.POST)
    @WebLog(description = "在项目中删除单个内容")
    public ResponseResult projectDeleteContent(Principal principal, @MultiRequestBody long projectId, @MultiRequestBody String type, @MultiRequestBody long contentId) {
        boolean flag = projectService.projectDeleteContent(principal.getName(), projectId, type, contentId);
        if (!flag) // 失败返回错误信息
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        // 返回成功信息
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 获取单个项目的VO信息
     * @param id 要获取项目的ID
     * @return  将project实体结构转为分页好的VO格式发送给前端
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/get/single", method = RequestMethod.GET)
    @WebLog(description = "获取单个项目的VO")
    public ProjectVO projectGetSingle(@RequestParam long id) {
        // 根据传进来的ID找project，查找失败抛出异常
        Project project = projectDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
        return ProjectVOEntityMapper.MAPPER.project2ProjectVO(project);
    }
}
