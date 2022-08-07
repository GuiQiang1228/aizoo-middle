package aizoo.repository;

import aizoo.domain.ModelInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelInfoDAO extends JpaRepository<ModelInfo, Long>, PagingAndSortingRepository<ModelInfo, Long> {
    @Query(value = "SELECT * FROM model_info WHERE model_info.model_category_id = ?1 ORDER BY id ASC",
            nativeQuery = true)
    Page<ModelInfo> findByModelCategoryId(long categoryId, Pageable pageable);

    List<ModelInfo> findByModelCategoryId(Long id);
}
