package aizoo.repository;

import aizoo.domain.ApplicationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ApplicationResultDAO extends JpaRepository<ApplicationResult, Long>, PagingAndSortingRepository<ApplicationResult, Long> {
}
