package id.ac.ui.cs.advprog.mysawitbe.modules.healthcheck.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_check")
@Getter @Setter
public class HealthCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private String status;
    private LocalDateTime checkedAt;
}