package aizoo.repository;

import aizoo.domain.Component;
import aizoo.domain.Datasource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface DatasourceDAO extends JpaRepository<Datasource, Long>, PagingAndSortingRepository<Datasource, Long> {

    Page<Datasource> findByUserUsername(String username, Pageable pageable);

    Datasource findByName(String name);

    List<Datasource> findByUserUsernameAndPathIsNotNull(String username);

    List<Datasource> findByPrivacyAndUserUsernameNotAndTitleLike(String privacy,String username,String title);

    List<Datasource> findByUserUsernameAndTitleLikeAndPrivacy(String username, String title, String privacy);

    List<Datasource> findByUserUsernameNotAndPrivacyAndPathIsNotNull(String username, String privacy);

    List<Datasource> findByUserUsernameNotAndPrivacyAndTitleLikeAndPathIsNotNull(String username, String privacy, String keyword);

    List<Datasource> findByUserUsernameAndTitleLikeAndPathIsNotNull(String username, String keyword);

    @Query(value = "SELECT datasource.* FROM datasource JOIN namespace ON datasource.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1)  AND IF(?2 !='',datasource.privacy LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',datasource.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',datasource.update_time>=?4,1=1) AND IF(?5 !='',datasource.update_time<=?5,1=1) AND IF(?6 !='',datasource.name LIKE CONCAT('%',?6,'%'),1=1) AND user.username=?7  ORDER BY datasource.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM datasource  JOIN namespace ON datasource.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1)  AND IF(?2 !='',datasource.privacy LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',datasource.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',datasource.update_time>=?4,1=1) AND IF(?5 !='',datasource.update_time<=?5,1=1) AND IF(?6 !='',datasource.name LIKE CONCAT('%',?6,'%'),1=1) AND user.username=?7 ORDER BY datasource.create_time DESC",
            nativeQuery = true)
    Page<Datasource> searchDatasource(String namespace, String privacy, String desc, String startUpdateTime, String endUpdateTime, String name, String userName, Pageable pageable);

    @Query(value = "SELECT datasource.* FROM datasource JOIN namespace ON datasource.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1)  AND IF(?2 !='',datasource.privacy LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',datasource.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',datasource.update_time>=?4,1=1) AND IF(?5 !='',datasource.update_time<=?5,1=1)  AND IF(?6 !='',datasource.name LIKE CONCAT('%',?6,'%'),1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY datasource.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM datasource  JOIN namespace ON datasource.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1)  AND IF(?2 !='',datasource.privacy LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',datasource.description LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',datasource.update_time>=?4,1=1) AND IF(?5 !='',datasource.update_time<=?5,1=1)  AND IF(?6 !='',datasource.name LIKE CONCAT('%',?6,'%'),1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY datasource.create_time DESC",
            nativeQuery = true)
    Page<Datasource> adminSearchDatasource(String namespace, String privacy, String desc, String startUpdateTime, String endUpdateTime, String name, String owner, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "update datasource set datasource.description = ?2 where datasource.id = ?1", nativeQuery = true)
    void updateDesc(long id, String desc);

    @Modifying
    @Transactional
    @Query(value = "update datasource set datasource.example = ?2 where datasource.id = ?1", nativeQuery = true)
    void updateExample(long id, String example);
}