package aizoo.service;

import aizoo.domain.Datatype;
import aizoo.common.ComponentType;
import aizoo.repository.ComponentDAO;
import aizoo.repository.DatatypeDAO;
import aizoo.repository.NamespaceDAO;
import aizoo.common.Link;
import aizoo.viewObject.object.canLink.ConnectVO;
import aizoo.viewObject.object.canLink.ConnectNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("ValidationService")
public class ValidationService {
    private final static Logger logger = LoggerFactory.getLogger(ValidationService.class);

    @Value("${file.path}")
    String file_path;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    /**
     * 判断source和target类型是否能连接
     *
     * @param connectVO  需要判断的连接
     * @return Map<String, String>类型，失败{"result":"failure","reason":"失败原因"}，成功{"result":"success"}
     */
    public Map<String, String> getCanLink(ConnectVO connectVO) {
        logger.info("Start get Can Link");
        logger.info("getCanLink connectVO:{}",connectVO);

        Map<String, String> result = new HashMap<>();

        //1. 获取connect的起点和终点
        ConnectNodeVO sourceNodeInfo = connectVO.getSource();
        ConnectNodeVO targetNodeInfo = connectVO.getTarget();

        //获取connect的起点和终点的命名空间
        String fromNamespace = sourceNodeInfo.getNodeNamespace();
        String toNamespace = targetNodeInfo.getNodeNamespace();

        //2. 若其中有命名空间不存在，则返回失败信息
        if(!(namespaceDAO.existsByNamespace(fromNamespace) && namespaceDAO.existsByNamespace(toNamespace))){
            result.put("result","failure");
            result.put("reason","命名空间不存在");

            logger.info("getCanLink return:{}",result);
            logger.info("End get Can Link");
            return result;
        }

        //3. 判断是否是文件和加载器相连
        if(sourceNodeInfo.getComponentType()==ComponentType.DATASOURCE && targetNodeInfo.getComponentType()==ComponentType.DATALOADER){
            //判断datatype是否一致
            if(verifyDatatypeEqual(sourceNodeInfo.getEndpointDataType(),targetNodeInfo.getEndpointDataType())){
                result.put("result","success");
            }
            else {
                result.put("result","failure");
                result.put("reason","datatype不一致");
            }
            logger.info("getCanLink return:{}",result);
            logger.info("End get Can Link");
            return result;
        }

        //4. 获取所有连接列表
        List<Link> linkList = connectVO.getLinklist();
        String sourceEndpointId = connectVO.getSource().getEndpointId();
        String targetEndpointId = connectVO.getTarget().getEndpointId();

        //判断是否重复连接，若重复连接则返回错误信息
        for(Link link : linkList){
            if(link.getSource().equals(sourceEndpointId) && link.getTarget().equals(targetEndpointId)){
                result.put("result","failure");
                result.put("reason","不能重复连接");
                logger.info("getCanLink return:{}",result);
                logger.info("End get Can Link");
                return result;
            }
        }
        //5. 判断datatype是否一致
        if(!verifyDatatypeEqual(sourceNodeInfo.getEndpointDataType(),targetNodeInfo.getEndpointDataType())){
            result.put("result","failure");
            result.put("reason","datatype不一致");
        }
        else {
            result.put("result","success");
        }
        logger.info("getCanLink return:{}",result);
        logger.info("End get Can Link");

        //返回结果
        return result;
    }

    /**
     * 判断source和target类型是否相同
     *
     * @param sourceDatatype 连接源点的数据类型
     * @param targetDatatype 连接终点的数据类型
     * @return boolean类型，source和target类型是否相同
     */
    public boolean verifyDatatypeEqual(String sourceDatatype, String targetDatatype){
        //获取源点和终点的Datatype实体
        Datatype source = datatypeDAO.findByName(sourceDatatype);
        Datatype target = datatypeDAO.findByName(targetDatatype);

        logger.info("getCanLink return:{}",source==target);
        logger.info("End get Can Link");

        return source == target;
    }
}
