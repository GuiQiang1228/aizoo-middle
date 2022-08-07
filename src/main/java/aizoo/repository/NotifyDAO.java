package aizoo.repository;

import aizoo.common.notifyEnum.NotifyType;
import aizoo.domain.Notify;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NotifyDAO extends JpaRepository<Notify, Long>, PagingAndSortingRepository<Notify, Long> {

    List<Notify> findByTypeAndCreateTimeAfter(NotifyType type, Date time, Sort sort);

    Notify findTopByTypeOrderByCreateTimeAsc(NotifyType type);

    Integer countByType(NotifyType type);

    Integer countByTypeAndCreateTimeAfter(NotifyType type, Date date);
}
