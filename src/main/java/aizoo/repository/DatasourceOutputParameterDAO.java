package aizoo.repository;

import aizoo.domain.DatasourceOutputParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface DatasourceOutputParameterDAO extends JpaRepository<DatasourceOutputParameter, Long> {
    @Modifying
    @Transactional
    @Query(value = "update datasource_output_parameter set datasource_output_parameter.description = ?2 where datasource_output_parameter.id = ?1", nativeQuery = true)
    void updateDesc(long id, String desc);
}
