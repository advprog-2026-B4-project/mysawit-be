package id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.repository;

import id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.model.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {
}