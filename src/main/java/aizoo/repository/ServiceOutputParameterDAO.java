package aizoo.repository;

import aizoo.domain.ServiceOutputParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOutputParameterDAO extends JpaRepository<ServiceOutputParameter, Long> {
}
