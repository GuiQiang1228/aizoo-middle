package aizoo.repository;


import aizoo.domain.ForkEdition;
import aizoo.domain.Level;

import aizoo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ForkEditionDAO extends JpaRepository<ForkEdition, Long> {
    ForkEdition findByUserId(long userid);
}
