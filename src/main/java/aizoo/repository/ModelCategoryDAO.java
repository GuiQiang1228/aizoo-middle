package aizoo.repository;

import aizoo.domain.ModelCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ModelCategoryDAO extends JpaRepository<ModelCategory, Long>, PagingAndSortingRepository<ModelCategory, Long> {
}
