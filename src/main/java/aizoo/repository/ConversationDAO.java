package aizoo.repository;

import aizoo.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ConversationDAO extends JpaRepository<Conversation, Long>, PagingAndSortingRepository<Conversation, Long> {

    Page<Conversation> findByOwnerUsername(String username, Pageable pageable);

    Conversation findByOwnerUsernameAndParticipantUsername(String owner, String participant);

    Integer countByOwnerUsernameAndUnreadCountGreaterThan(String username, Integer number);   // 统计未读消息的会话数
}
