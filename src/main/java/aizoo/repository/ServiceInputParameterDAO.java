package aizoo.repository;

import aizoo.domain.ServiceInputParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceInputParameterDAO extends JpaRepository<ServiceInputParameter, Long> {
}
