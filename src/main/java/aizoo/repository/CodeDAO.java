package aizoo.repository;

import aizoo.domain.Code;
import aizoo.domain.MirrorJob;
import aizoo.domain.User;
import aizoo.viewObject.object.CodeVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeDAO extends JpaRepository<Code, Long>, PagingAndSortingRepository<Code, Long> {

    @Query(value = "SELECT code.* FROM code JOIN user ON code.user_id=user.id WHERE IF(?1 != '',code.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',code.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',code.update_time>=?3,1=1) AND IF(?4 !='',code.update_time<=?4,1=1) AND user.username=?5 ORDER BY code.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM code JOIN user ON code.user_id=user.id WHERE IF(?1 != '',code.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',code.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',code.update_time>=?3,1=1) AND IF(?4 !='',code.update_time<=?4,1=1) AND user.username=?5 ORDER BY code.create_time DESC",
            nativeQuery = true)
    Page<Code> searchCode(String name, String description,  String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    List<Code> findByUserUsername(String username);

    Code findByName(String name);

    Code findByNameAndUserUsername(String codeName, String userName);
}
