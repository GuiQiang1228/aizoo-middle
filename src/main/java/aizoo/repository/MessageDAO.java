package aizoo.repository;

import aizoo.domain.Conversation;
import aizoo.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MessageDAO extends JpaRepository<Message, Long>, PagingAndSortingRepository<Message, Long> {

    Page<Message> findMessagesByConversationsContainsAndIdIsLessThan(Conversation conversation, Long afterId, Pageable pageable);
}
