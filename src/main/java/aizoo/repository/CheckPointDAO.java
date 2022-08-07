package aizoo.repository;

import aizoo.domain.CheckPoint;
import aizoo.domain.MirrorJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckPointDAO extends JpaRepository<CheckPoint, Long>, PagingAndSortingRepository<CheckPoint, Long> {
    List<CheckPoint> findByUserUsername(String username);

    List<CheckPoint> findByNameAndExperimentJobIdAndAndNamespaceNamespace(String name, long experimentJobId, String namespace);

    List<CheckPoint> findByName(String name);
    @Query(value = "SELECT check_point.* FROM check_point JOIN user ON check_point.user_id=user.id WHERE IF(?1 != '',check_point.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',check_point.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',check_point.update_time>=?3,1=1) AND IF(?4 !='',check_point.update_time<=?4,1=1) AND user.username=?5 ORDER BY check_point.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM check_point JOIN user ON check_point.user_id=user.id WHERE IF(?1 != '',check_point.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',check_point.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',check_point.update_time>=?3,1=1) AND IF(?4 !='',check_point.update_time<=?4,1=1) AND user.username=?5 ORDER BY check_point.create_time DESC",
            nativeQuery = true)
    Page<CheckPoint> searchCheckPoint(String name, String description,  String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);
}
