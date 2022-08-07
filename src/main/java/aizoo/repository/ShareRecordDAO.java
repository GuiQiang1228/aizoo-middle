package aizoo.repository;

import aizoo.domain.ShareRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShareRecordDAO extends JpaRepository<ShareRecord, Long>, PagingAndSortingRepository<ShareRecord, Long> {

    Page<ShareRecord> findByRecipientUsername(String username, Pageable pageable);

    List<ShareRecord> findByRecipientUsername(String username);

    Page<ShareRecord> findBySenderUsername(String username, Pageable pageable);

    ShareRecord findBySenderUsernameAndRecipientUsernameAndResourceId(String sender, String recipient, Long resourceId);

    Integer countByRecipientUsernameAndIsRead(String username, boolean isRead);

    @Query(value = "SELECT share_record.* FROM share_record JOIN user ON share_record.sender_id=user.id JOIN graph ON share_record.resource_id=graph.id WHERE IF(?1 != '',graph.name like CONCAT('%',?1,'%') or graph.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.sender_id=user.id JOIN datasource ON share_record.resource_id=datasource.id WHERE IF(?1 != '',datasource.name like CONCAT('%',?1,'%') or datasource.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.sender_id=user.id JOIN component ON share_record.resource_id=component.id  WHERE IF(?1 != '',component.name like CONCAT('%',?1,'%') or component.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.sender_id=user.id or share_record.recipient_id=user.id WHERE IF(?1 != '', user.email LIKE CONCAT('%',?1,'%') or user.username like CONCAT('%',?1,'%'), 1=1) ORDER BY create_time DESC",
            countQuery = "SELECT COUNT(1) FROM share_record JOIN user ON share_record.sender_id=user.id or share_record.recipient_id=user.id WHERE IF(?1 != '', user.email LIKE CONCAT('%',?1,'%') or user.username like CONCAT('%',?1,'%'), 1=1) " +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.sender_id=user.id JOIN component ON share_record.resource_id=component.id  WHERE IF(?1 != '',component.name like CONCAT('%',?1,'%') or component.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.sender_id=user.id JOIN datasource ON share_record.resource_id=datasource.id WHERE IF(?1 != '',datasource.name like CONCAT('%',?1,'%') or datasource.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2" +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.sender_id=user.id JOIN graph ON share_record.resource_id=graph.id WHERE IF(?1 != '',graph.name like CONCAT('%',?1,'%') or graph.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 )",
            nativeQuery = true)
    Page<ShareRecord> sendSearchShareRecord(String name, String username, Pageable pageable);

    @Query(value = "SELECT share_record.* FROM share_record JOIN user ON share_record.sender_id=user.id or share_record.recipient_id=user.id WHERE IF(?1 != '', user.email LIKE CONCAT('%',?1,'%') or user.username like CONCAT('%',?1,'%'), 1=1)" +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN graph ON share_record.resource_id=graph.id WHERE IF(?1 != '',graph.name like CONCAT('%',?1,'%') or graph.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN component ON share_record.resource_id=component.id  WHERE IF(?1 != '',component.name like CONCAT('%',?1,'%') or component.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
            "UNION SELECT share_record.* FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN datasource ON share_record.resource_id=datasource.id WHERE IF(?1 != '',datasource.name like CONCAT('%',?1,'%') or datasource.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2  ORDER BY create_time DESC",
            countQuery = "SELECT COUNT(1) FROM share_record JOIN user ON share_record.sender_id=user.id or share_record.recipient_id=user.id WHERE IF(?1 != '', user.email LIKE CONCAT('%',?1,'%') or user.username like CONCAT('%',?1,'%'), 1=1)" +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN component ON share_record.resource_id=component.id  WHERE IF(?1 != '',component.name like CONCAT('%',?1,'%') or component.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 " +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN datasource ON share_record.resource_id=datasource.id WHERE IF(?1 != '',datasource.name like CONCAT('%',?1,'%') or datasource.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2" +
                    "UNION SELECT COUNT(1) FROM share_record JOIN user ON share_record.recipient_id=user.id JOIN graph ON share_record.resource_id=graph.id WHERE IF(?1 != '',graph.name like CONCAT('%',?1,'%') or graph.description like CONCAT('%',?1,'%'), 1=1) AND user.username = ?2 )",
            nativeQuery = true)
    Page<ShareRecord> acceptSearchShareRecord(String name, String username, Pageable pageable);

}
