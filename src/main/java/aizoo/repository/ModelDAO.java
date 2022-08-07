package aizoo.repository;


import aizoo.domain.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelDAO extends JpaRepository<Model, Long>, PagingAndSortingRepository<Model, Long> {
}
