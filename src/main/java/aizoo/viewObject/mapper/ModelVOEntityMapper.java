package aizoo.viewObject.mapper;


import aizoo.domain.Graph;
import aizoo.domain.Model;
import aizoo.viewObject.object.ModelVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;



@Mapper(componentModel = "spring")
public interface ModelVOEntityMapper {
    ModelVOEntityMapper MAPPER = Mappers.getMapper(ModelVOEntityMapper.class);
    @Mappings({
            @Mapping(target = "graphId", expression = "java(model.getGraph().getId())"),
            @Mapping(target = "username", expression = "java(model.getUser().getUsername())"),
            @Mapping(target = "graphType", expression = "java(model.getGraph().getGraphType())")}
    )
    ModelVO model2ModelVO(Model model);

    /**
     * Model->ModelVO,完成时补全属性
     * @param model
     * @param modelVO
     */
    @AfterMapping
    default void completeModelVO(Model model, @MappingTarget ModelVO modelVO){
        Graph graph = model.getGraph();
        if(graph.getComponent() != null){
            modelVO.setSourceId(graph.getComponent().getId());
        }
        else if(graph.getService() != null)
            modelVO.setSourceId(graph.getService().getId());
        else
            modelVO.setSourceId(graph.getId());
    }
}
