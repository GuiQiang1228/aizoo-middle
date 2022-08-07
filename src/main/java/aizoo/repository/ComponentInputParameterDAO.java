package aizoo.repository;

import aizoo.domain.ComponentInputParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface ComponentInputParameterDAO extends JpaRepository<ComponentInputParameter, Long> {
    @Modifying
    @Transactional
    @Query(value = "update component_input_parameter set component_input_parameter.description = ?2 where component_input_parameter.id = ?1", nativeQuery = true)
    void updateDesc(long id, String desc);
}
