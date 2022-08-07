package aizoo.utils;

import aizoo.domain.*;
import aizoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service("DAOUtil")
public class DAOUtil {
    /**
     * 访问数据库，根据id或name对数据库中的内容进行查找
     * @param id
     * @param name
     *
     * 若查找到满足条件的实体
     * @return 所查找到的实体
     * 若没有查找到满足条件的实体
     * @throws EntityNotFoundException
     */

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    CheckPointDAO checkPointDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    GraphDAO graphDAO;

    // 根据id查找datasource数据库中相关内容
    public Datasource findDatasourceById(Long id){
        return datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据id查找checkPoint数据库中相关内容
    public CheckPoint findCheckPointById(Long id){
        return checkPointDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据id查找service数据库中相关内容
    public aizoo.domain.Service findServiceById(Long id){
        return serviceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据id查找serviceJob数据库中相关内容
    public ServiceJob findServiceJobById(Long id){
        return serviceJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据id查找visualContainer数据库中相关内容
    public VisualContainer findVisualContainerById(Long id){
        return visualContainerDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据name查找datatype数据库中相关内容
    public Datatype findDataTypeByName(String name){
        return datatypeDAO.findByName(name);
    }

    // 根据id查找datatype数据库中相关内容
    public Datatype findDataTypeById(Long id){
        Datatype datatype = datatypeDAO.findById( id ).orElseThrow(()-> new EntityNotFoundException(String.valueOf(id)));
        return datatype;
    }

    // 根据id查找graph数据库中相关内容
    public Graph findGraphById(Long id){
        return graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    // 根据id查找component数据库中相关内容
    public Component findComponentById(Long id){
        return componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }
}
