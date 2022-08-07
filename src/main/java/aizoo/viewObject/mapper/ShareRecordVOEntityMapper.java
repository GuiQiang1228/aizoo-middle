package aizoo.viewObject.mapper;

import aizoo.domain.Component;
import aizoo.domain.Datasource;
import aizoo.domain.Graph;
import aizoo.domain.ShareRecord;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.object.ShareRecordVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;

@Mapper(componentModel = "spring")
public interface ShareRecordVOEntityMapper {

    ShareRecordVOEntityMapper MAPPER = Mappers.getMapper(ShareRecordVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    target = "sender",
                    expression = "java(shareRecord.getSender().getUsername())"),
            @Mapping(
                    target = "recipient",
                    expression = "java(shareRecord.getRecipient().getUsername())"),
    })
    ShareRecordVO shareRecord2ShareRecordVO(ShareRecord shareRecord, @Context DAOUtil daoUtil);

    @AfterMapping
    default void completeShareRecordVO(@MappingTarget ShareRecordVO shareRecordVO, ShareRecord shareRecord, @Context DAOUtil daoUtil){
        String resourceName = null;
        String description = null;
        shareRecordVO.setExisted(true);
        try{
            switch(shareRecord.getResourceType()){
                case COMPONENT: {
                    Component component = daoUtil.findComponentById(shareRecord.getResourceId());
                    resourceName = component.getName();
                    description = component.getDescription();
                    break;
                }
                case DATASOURCE: {
                    Datasource datasource = daoUtil.findDatasourceById(shareRecord.getResourceId());
                    resourceName = datasource.getName();
                    description = datasource.getDescription();
                    break;
                }
                case COMPONENT_GRAPH:;
                case SERVICE_GRAPH:;
                case APPLICATION_GRAPH:;
                case EXPERIMENT_GRAPH: {
                    Graph graph = daoUtil.findGraphById(shareRecord.getResourceId());
                    resourceName = graph.getName();
                    description = "";
                    break;
                }
                default: resourceName = "无资源名"; description = "无描述信息"; break;
            }
        }catch (EntityNotFoundException e){  // 处理资源被 分享者 所删除的情况
            resourceName = "无资源名";
            description = "资源已被删除";
            shareRecordVO.setExisted(false);
        }
        shareRecordVO.setResourceName(resourceName);
        shareRecordVO.setDescription(description);
        shareRecordVO.setRecipientEmail(shareRecord.getRecipient().getEmail());
        shareRecordVO.setSenderEmail(shareRecord.getSender().getEmail());

    }
}
