package aizoo.repository;

import aizoo.domain.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDAO extends JpaRepository<Service, Long>, PagingAndSortingRepository<Service, Long> {
    @Query(value = "SELECT service.*,graph.name FROM service JOIN graph ON service.graph_id=graph.id JOIN user ON service.user_id=user.id WHERE IF(?1 != '',service.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND user.username=?5 ORDER BY service.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM service JOIN graph ON service.graph_id=graph.id JOIN user ON service.user_id=user.id WHERE IF(?1 != '',service.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND user.username=?5 ORDER BY service.create_time DESC",
            nativeQuery = true)
    Page<Service> searchService(String serviceName, String description, String jobStatus, String graphName, String userName, Pageable pageable);

    List<Service> findByUserUsernameAndReleasedIsTrue(String username);

    List<Service> findByUserUsernameAndReleased(String username, boolean released);

    List<Service> findByUserUsername(String username);

    List<Service> findByUserUsernameAndTitleLikeAndPrivacy(String username,String title,String privacy);
}
