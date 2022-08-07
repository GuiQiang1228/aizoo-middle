package aizoo.repository;

import aizoo.domain.Graph;
import aizoo.common.ComponentType;
import aizoo.common.GraphType;
import aizoo.viewObject.object.GraphVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface GraphDAO extends JpaRepository<Graph, Long>, PagingAndSortingRepository<Graph, Long> {
    Graph findByGraphKey(String graphKey);

    boolean existsById(Long graphId);

    List<Graph> findByUserUsernameAndGraphType(String userName, GraphType graphType);

    List<Graph> findByUserUsernameAndGraphTypeAndNameAndReleased(String userName, GraphType graphType, String name, boolean released);

    Page<Graph> findAllByUserUsernameAndGraphType(String userName, GraphType graphType, Pageable pageable);

    Graph findByNameAndUserUsername(String name, String userName);

    Page<Graph> findByComponentComponentTypeAndUserUsername(ComponentType componentType, String username, Pageable pageable);

    Graph findByExperimentJobsId(Long jobId);

    int countByUserUsername(String username);

    boolean existsByNameAndUserUsername(String name, String username);

    Graph findByServiceId(Long serviceId);

    @Query(value = "SELECT graph.*  FROM graph JOIN user ON graph.user_id=user.id WHERE IF(?1 != '',graph.graph_type like CONCAT('%',?1,'%'),1=1) AND IF(?2 !='',graph.name LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',graph.update_time>=?4,1=1) AND IF(?5 !='',graph.update_time<=?5,1=1) AND IF(?7 !='',graph.released=?7,1=1) AND IF(?8!='', graph.graph_version LIKE CONCAT('%',?8,'%'),1=1) AND user.username=?6 ORDER BY graph.create_time DESC",
            countQuery = "SELECT COUNT(1)  FROM graph  JOIN user ON graph.user_id=user.id WHERE IF(?1 != '',graph.graph_type like CONCAT('%',?1,'%'),1=1) AND IF(?2 !='',graph.name LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',graph.update_time>=?4,1=1) AND IF(?5 !='',graph.update_time<=?5,1=1) AND IF(?7 !='',graph.released=?7,1=1) AND IF(?8!='', graph.graph_version LIKE CONCAT('%',?8,'%'),1=1) AND user.username=?6 ORDER BY graph.create_time DESC",
            nativeQuery = true)
    Page<Graph> searchGraph(String type, String graphName, String desc, String startUpdateTime, String endUpdateTime, String userName, String releaseStatus, String version, Pageable pageable);

    @Query(value = "SELECT graph.*  FROM graph JOIN component ON component.graph_id=graph.id  JOIN user ON component.user_id=user.id WHERE  IF(?1 != '',graph.graph_version LIKE CONCAT('%',?1,'%'),1=1)AND IF(?2 !='',graph.released=?2,1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',graph.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',graph.update_time>=?5,1=1) AND IF(?6 !='',graph.update_time<=?6,1=1) AND IF(?7 !='',component.component_type LIKE CONCAT('%',?7,'%'),1=1) AND user.username=?8 ORDER BY graph.create_time DESC",
            countQuery = "SELECT COUNT(1)  FROM graph JOIN component ON component.graph_id=graph.id JOIN user ON component.user_id=user.id WHERE  IF(?1 != '',graph.graph_version LIKE CONCAT('%',?1,'%'),1=1)AND IF(?2 !='',graph.released=?2,1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',graph.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',graph.update_time>=?5,1=1) AND IF(?6 !='',graph.update_time<=?6,1=1) AND IF(?7 !='',component.component_type LIKE CONCAT('%',?7,'%'),1=1) AND user.username=?8  ORDER BY graph.create_time DESC",
            nativeQuery = true)
    Page<Graph> searchComponent(String version, String releaseStatus, String graphName, String desc, String startUpdateTime, String endUpdateTime, String type, String userName, Pageable pageable);
}
