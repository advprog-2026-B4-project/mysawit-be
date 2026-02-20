package id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.model.HealthCheck;
import id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.repository.HealthCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthCheckService {

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    public List<HealthCheck> getHealthStatus() {
        List<HealthCheck> checks = healthCheckRepository.findAll();
        checks.forEach(hc -> hc.setCheckedAt(LocalDateTime.now()));
        healthCheckRepository.saveAll(checks);
        return checks;
    }
}
