package aizoo.repository;


import aizoo.domain.ComponentOutputParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface ComponentOutputParameterDAO extends JpaRepository<ComponentOutputParameter, Long> {
    @Modifying
    @Transactional
    @Query(value = "update component_output_parameter set component_output_parameter.description = ?2 where component_output_parameter.id = ?1", nativeQuery = true)
    void updateDesc(long id, String desc);
}
