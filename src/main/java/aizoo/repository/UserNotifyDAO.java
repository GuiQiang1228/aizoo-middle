package aizoo.repository;

import aizoo.common.notifyEnum.NotifyType;
import aizoo.domain.UserNotify;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotifyDAO extends JpaRepository<UserNotify, Long>, PagingAndSortingRepository<UserNotify, Long> {

    UserNotify findTopByUserUsernameAndNotifyType(String username, NotifyType type, Sort sort);

    Page<UserNotify> findByUserUsernameAndNotifyType(String username, NotifyType type, Pageable pageable);

    Integer countByUserUsernameAndNotifyTypeAndIsRead(String username, NotifyType type, boolean isRead);

    Integer countByUserUsernameAndNotifyTypeNotAndIsRead(String username, NotifyType type, boolean isRead);

    void deleteUserNotifyById(Long id);

    void deleteAllByUserUsernameAndNotifyType(String username, NotifyType type);
}
