package aizoo.repository;


import aizoo.domain.Datatype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface DatatypeDAO extends JpaRepository<Datatype, Long> {
    Datatype findByName(String name);

    //Datatype findById(Long id);

    boolean existsByName(String name);
}
