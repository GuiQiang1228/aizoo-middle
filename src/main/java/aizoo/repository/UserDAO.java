package aizoo.repository;

import aizoo.domain.Application;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.common.UserRoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDAO extends JpaRepository<User, Long>, PagingAndSortingRepository<User, Long> {

    User findByUsername(String username);

    User findByEmail(String email);

    @Query(value = "SELECT user.* FROM user JOIN user_roles ON user.id=user_roles.user_id JOIN role ON role.id=user_roles.role_id JOIN level ON level.id=user.level_id WHERE IF(?1 != '',user.username like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',level.name LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',role.user_role_type LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',user.level_finish_time<=?4,1=1) ORDER BY user.level_id DESC",
            countQuery = "SELECT COUNT(1) FROM user JOIN user_roles ON user.id=user_roles.user_id JOIN role ON role.id=user_roles.role_id JOIN level ON level.id=user.level_id WHERE IF(?1 != '',user.username like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',level.name LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',role.user_role_type LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',user.level_finish_time<=?4,1=1) ORDER BY user.level_id DESC",
            nativeQuery = true)
    Page<User> searchUser(String username, String level, String roles, String levelTime, Pageable pageable);


    List<User> findByRolesUserRoleType(UserRoleType userRoleType);

    User findBySlurmAccountId(long slurmAccountId);

    List<User> findBySlurmAccountNotNull();

}
