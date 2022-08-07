package aizoo.repository;

import aizoo.common.LevelChangeType;
import aizoo.domain.UserLevelChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLevelChangeLogDAO extends JpaRepository<UserLevelChangeLog, Long> {
    List<UserLevelChangeLog> findByUserUsernameAndChanged(String username, boolean changed);

    List<UserLevelChangeLog> findByUserUsernameAndChangeType(String username, LevelChangeType changeType);
}
