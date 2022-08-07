package aizoo.repository;

import aizoo.domain.SlurmAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlurmAccountDAO extends JpaRepository<SlurmAccount, Long>, PagingAndSortingRepository<SlurmAccount, Long> {
    SlurmAccount findByUsernameAndIp(String username, String ip);
}
