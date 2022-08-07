package aizoo.repository;

import aizoo.domain.VisualContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface VisualContainerDAO extends JpaRepository<VisualContainer, Long>, PagingAndSortingRepository<VisualContainer, Long> {
    List<VisualContainer> findByTitleLike(String keyword);

    VisualContainer findByName(String name);
}
