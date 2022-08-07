package aizoo.repository;

import aizoo.domain.Code;
import aizoo.domain.Mirror;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MirrorDAO extends JpaRepository<Mirror, Long>, PagingAndSortingRepository<Mirror, Long> {

    List<Mirror> findAll();

    Mirror findByName(String name);

    Mirror findByUserUsernameAndName(String userName, String name);

    List<Mirror> findByPrivacyAndPathIsNotNull(String privacy);

    List<Mirror> findByPrivacyOrUserUsername(String privacy, String userName);
}
