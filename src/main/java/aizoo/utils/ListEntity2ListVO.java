package aizoo.utils;

import aizoo.domain.*;
import aizoo.viewObject.mapper.*;
import aizoo.viewObject.object.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//该类主要用于将ListEntity转换为ListVO
public class ListEntity2ListVO {

    /**
     *该方法用于将ComponentMap转换为ComponentVOMap以便前端使用
     * @param componentMap 待转换的componentMap，value值为存储component的list
     * @return
     */
    public static HashMap<String,List<ComponentVO>> componentMap2ComponentVOMap(HashMap<String,List<Component>> componentMap){
        HashMap<String,List<ComponentVO>> componentVOMap = new HashMap<>();
        //遍历componentMap
        for(String componentVOType:componentMap.keySet()){
            //key是componentVOType，存入新的componentVOMap中
            //调用component2ComponentVO方法直接实现list类型的转换
            componentVOMap.put(componentVOType,component2ComponentVO(componentMap.get(componentVOType)));
        }
        return componentVOMap;
    }

    /**
     * 该方法用于将存储Component的list转换为存储ComponentVO的list
     * @param componentList 待转换的存放component的list
     * @return 返回值必须为存放componentVO的list
     */
    public static List<ComponentVO> component2ComponentVO(List<Component> componentList){
        List<ComponentVO> componentVOList = new ArrayList<>();
        if (!componentList.isEmpty()){
            //遍历componentList
            for(Component component:componentList){
                //调用mapper中的方法实现转换并加入componentVOList中
                componentVOList.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(component));
            }
        }
        return componentVOList;
    }

    /**
     * 该方法用于将存储component的list转换为存储componentVO的list
     * @param componentList 待转换的存放component的list
     * @return 返回值为list
     */
    public static List<Object> component2ComponentVOObject(List<Component> componentList){
        List<Object> componentVOList = new ArrayList<>();
        if (!componentList.isEmpty()){
            for(Component component:componentList){
                componentVOList.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(component));
            }
        }
        return componentVOList;
    }

    /**
     * 该方法用于将存放component的list和存放datasource的list转换为存放componentVO和datasourceVO的list
     * @param componentList 待转换的存放component的list
     * @param datasourceList 待转换的存放datasource的list
     * @return 返回包含componentVO和datasourceVO的一个list
     */
    public static List<Object> componentAndDatasource2ComponentVOAndDatasourceVOObject(List<Component> componentList,List<Datasource> datasourceList){
        List<Object> resultVOList = new ArrayList<>();
        if (!componentList.isEmpty()){
            //遍历componentList
            for(Component component:componentList){
                //调用component2ComponentVO方法转换并加入resultVOList
                resultVOList.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(component));
            }
        }
        if (!datasourceList.isEmpty()){
            //遍历datasourceList
            for(Datasource datasource:datasourceList){
                //调用datasource2DatasourceVO方法转换并加入resultVOList
                resultVOList.add(DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(datasource));
            }
        }
        return resultVOList;
    }

    /**
     * 实现组件、可视化、服务和数据资源list到对应VO的list的转换
     * @param componentList 待转换的componentList
     * @param visualContainerList 待转换的visualContainerList
     * @param serviceList 待转换的serviceList
     * @param datasourceList 待转换的datasourceList
     * @return 返回包含所有转换后数据的一个list
     */
    public static List<Object> componentAndVisualContainerAndServiceAndDatasource2ComponentVOAndVisualContainerAndServiceVOAndDatasourceVOObject(List<Component> componentList,List<VisualContainer> visualContainerList,List<Service> serviceList,List<Datasource> datasourceList){
        List<Object> resultVOList = new ArrayList<>();
        if (!componentList.isEmpty()){
            for(Component component:componentList){
                resultVOList.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(component));
            }
        }
//        可视化暂无Mapper转换
        if (!datasourceList.isEmpty()){
            for(Datasource datasource:datasourceList){
                resultVOList.add(DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(datasource));
            }
        }
        if (!serviceList.isEmpty()){
            for(Service service:serviceList){
                resultVOList.add(ServiceVOEntityMapper.MAPPER.Service2ServiceVO(service));
            }
        }
        return resultVOList;
    }

    /**
     * 本方法用于实现graphList到graphVOList的转换
     * @param graphList 待转换的graphList
     * @return 转换后的graphVOList
     */
    public static List<GraphVO> graph2GraphVO(List<Graph> graphList){
        List<GraphVO> graphVOList = new ArrayList<>();
        if(!graphList.isEmpty()){
            for(Graph graph:graphList){
                graphVOList.add(GraphVOEntityMapper.MAPPER.graph2GraphVO(graph));
            }
        }
        return graphVOList;
    }

    /**
     * 本方法用于实现datasourceList到datasourceVOList的转换
     * @param datasourceList 待转换的datasourceList
     * @return 转换后的datasourceVOList
     */
    public static List<DatasourceVO> datasource2DatasourceVO(List<Datasource> datasourceList){
        List<DatasourceVO> datasourceVOList = new ArrayList<>();
        if(!datasourceList.isEmpty()){
            for(Datasource datasource:datasourceList){
                datasourceVOList.add(DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(datasource));
            }
        }
        return datasourceVOList;
    }

    /**
     * 本方法用于实现containerList到containerVOList的转换
     * @param containerList 待转换的containerList
     * @return 转换后的containerVOList
     */
    public static List<VisualContainerVO> container2ContainerVO(List<VisualContainer> containerList){
        List<VisualContainerVO> containerVOList = new ArrayList<>();
        if(!containerList.isEmpty()){
            for(VisualContainer container:containerList){
                containerVOList.add(ContainerVOEntityMapper.MAPPER.container2ContainerVO(container));
            }
        }
        return containerVOList;
    }

    /**
     * 本方法用于实现serviceList到serviceVOList的转换
     * @param serviceList 待转换的serviceList
     * @return 转换后的serviceVOList
     */
    public static List<ServiceVO> service2ServiceVO(List<Service> serviceList){
        List<ServiceVO> serviceVOList = new ArrayList<>();
        if(!serviceList.isEmpty()){
            for(Service service:serviceList){
                serviceVOList.add(ServiceVOEntityMapper.MAPPER.Service2ServiceVO(service));
            }
        }
        return serviceVOList;
    }

    /**
     * 本方法用于实现checkPointList到checkPointVOList的转换
     * @param checkPointList
     * @return 转换后的checkPointVOList
     */
    public static List<CheckPointVO> checkPoint2CheckPointVO(List<CheckPoint> checkPointList){
        List<CheckPointVO> checkPointVOList = new ArrayList<>();
        if(!checkPointList.isEmpty()){
            for(CheckPoint checkPoint:checkPointList){
                checkPointVOList.add(CheckPointVOEntityMapper.MAPPER.CheckPoint2CheckPointVO(checkPoint));
            }
        }
        return checkPointVOList;
    }

    /**
     * 本方法用于实现experimentJobList到experimentJobVOList的转换
     * @param experimentJobList 待转换的experimentJobList
     * @return 转换后的experimentJobVOList
     */
    public static List<ExperimentJobVO> job2JobVO(List<ExperimentJob> experimentJobList){
        List<ExperimentJobVO> experimentJobVOList = new ArrayList<>();
        if (!experimentJobList.isEmpty()){
            for(ExperimentJob experimentJob : experimentJobList){
                experimentJobVOList.add(ExperimentJobVOEntityMapper.MAPPER.job2JobVO(experimentJob));
            }
        }
        return experimentJobVOList;
    }

    /**
     * 本方法用于实现experimentJobCheckpointList到experimentJobCheckpointVOList的转换
     * @param experimentJobCheckpointList 待转换的experimentJobCheckpointList
     * @return 转换后的experimentJobCheckpointVOList
     */
    public static List<ExperimentJobCheckpointVO> jobCheckpoint2JobCheckpointVO(List<ExperimentJob> experimentJobCheckpointList){
        List<ExperimentJobCheckpointVO> experimentJobCheckpointVOList = new ArrayList<>();
        if (!experimentJobCheckpointList.isEmpty()){
            for(ExperimentJob experimentJob :experimentJobCheckpointList){
                experimentJobCheckpointVOList.add(ExperimentJobCheckpointVOEntityMapper.MAPPER.jobCheckpoint2JobCheckpointVO(experimentJob));
            }
        }
        return experimentJobCheckpointVOList;
    }

    /**
     * 本方法用于实现serviceJobList到serviceJobVOList的转换
     * @param serviceJobList 待转换的serviceJobList
     * @return 转换后的serviceJobVOList
     */
    public static List<ServiceJobVO> serviceJob2ServiceJobVO(List<ServiceJob> serviceJobList){
        List<ServiceJobVO> serviceJobVOList = new ArrayList<>();
        if(!serviceJobList.isEmpty()){
            for(ServiceJob serviceJob: serviceJobList){
                serviceJobVOList.add(ServiceJobVOEntityMapper.MAPPER.serviceJob2ServiceJobVO(serviceJob));
            }
        }
        return serviceJobVOList;
    }

    /**
     * 本方法用于实现RunningServiceJobList到serviceJobVOList的转换
     * @param RunningServiceJobList 待转换的RunningServiceJobList
     * @return 转换后的runningServiceJobVOList
     */
    public static List<RunningServiceJobVO> serviceJob2RunningServiceJobVO(List<ServiceJob> RunningServiceJobList){
        List<RunningServiceJobVO> runningServiceJobVOList = new ArrayList<>();
        if(!RunningServiceJobList.isEmpty()){
            for(ServiceJob serviceJob: RunningServiceJobList){
                runningServiceJobVOList.add(RunningServiceJobVOEntityMapper.MAPPER.Entity2VO(serviceJob));
            }
        }
        return runningServiceJobVOList;
    }

    /**
     * 该方法用于将存储Mirror的list转换为存储MirrorVO的list
     * @param mirrorList 待转换的存放component的list
     * @return 返回值必须为存放MirrorVO的list
     */
    public static List<MirrorVO> mirror2MirrorVO(List<Mirror> mirrorList){
        List<MirrorVO> mirrorVOList = new ArrayList<>();
        if (!mirrorList.isEmpty()){
            for(Mirror mirror : mirrorList){
                // 调用mapper中的方法实现转换并加入componentVOList中
                mirrorVOList.add(MirrorVOEntityMapper.MAPPER.mirror2MirrorVO(mirror));
            }
        }
        return mirrorVOList;
    }
}
