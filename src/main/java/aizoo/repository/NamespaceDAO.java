package aizoo.repository;

import aizoo.domain.ExperimentJob;
import aizoo.domain.Namespace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface NamespaceDAO extends JpaRepository<Namespace, Integer>, PagingAndSortingRepository<Namespace, Integer> {
    Namespace findByNamespace(String namespace);

    @Query(value = "SELECT namespace.* FROM namespace  JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',namespace.namespace LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',namespace.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',namespace.update_time>=?4,1=1) AND IF(?5 !='',namespace.update_time<=?5,1=1) AND user.username=?6 AND namespace.namespace not like CONCAT(?6,'.checkpoint','%')ORDER BY namespace.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM namespace  JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',namespace.namespace LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',namespace.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',namespace.update_time>=?4,1=1) AND IF(?5 !='',namespace.update_time<=?5,1=1) AND user.username=?6 AND namespace.namespace not like CONCAT(?6,'.checkpoint','%') ORDER BY namespace.create_time DESC",
            nativeQuery = true)
    Page<Namespace> searchNamespace(String namespace, String type, String privacy, String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    @Query(value = "SELECT namespace.* FROM namespace  JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',namespace.namespace LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',namespace.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',namespace.update_time>=?4,1=1) AND IF(?5 !='',namespace.update_time<=?5,1=1) AND IF(?6 !='',user.username=?6,1=1) AND namespace.namespace not like CONCAT(?6,'.checkpoint','%') ORDER BY namespace.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM namespace  JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',namespace.namespace LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',namespace.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',namespace.update_time>=?4,1=1) AND IF(?5 !='',namespace.update_time<=?5,1=1) AND IF(?6 !='',user.username=?6,1=1) AND namespace.namespace not like CONCAT(?6,'.checkpoint','%') ORDER BY namespace.create_time DESC",
            nativeQuery = true)
    Page<Namespace> adminSearchNamespace(String namespace, String type, String privacy, String startUpdateTime, String endUpdateTime, String owner, Pageable pageable);

    List<Namespace> findByUserUsernameAndNamespaceLike(String username, String namespace);

    List<Namespace> findByUserUsernameAndNamespaceLike(String username, String keywords, Sort sort);

    List<Namespace> findByUserUsernameAndNamespaceNotLikeAndNamespaceNotLikeAndNamespaceNotLike(String username, String keyword1, String keyword2,String keyword3, Sort sort);

    Page<Namespace> findByUserUsernameAndNamespaceNotLike(String userName,String keyword, Pageable pageable);

    Page<Namespace> findByNamespaceNotLike(String string, Pageable pageable);

    boolean existsByNamespace(String namespace);


}