package id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.controller;

import id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.model.HealthCheck;
import id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping
    public List<HealthCheck> getHealthStatus() {
        return healthCheckService.getHealthStatus();
    }
}