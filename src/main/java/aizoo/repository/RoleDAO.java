package aizoo.repository;

import aizoo.common.UserRoleType;
import aizoo.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleDAO extends JpaRepository<Role, Integer> {
    Role findByUserRoleType(UserRoleType userRoleType);
}
