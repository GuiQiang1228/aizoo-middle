package aizoo.repository;

import aizoo.common.LevelType;
import aizoo.domain.Application;
import aizoo.domain.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface LevelDAO extends JpaRepository<Level, Long>, PagingAndSortingRepository<Level, Long> {
    Level findByName(LevelType name);
}
