package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "payrolls",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_payrolls_user_role_reference",
				columnNames = {"user_id", "role", "reference_id", "reference_type"}
		)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "payroll_id", nullable = false, updatable = false)
	private UUID payrollId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "role", nullable = false, length = 20)
	private String role;

	@Column(name = "reference_id", nullable = false)
	private UUID referenceId;

	@Column(name = "reference_type", nullable = false, length = 20)
	private String referenceType;

	@Column(name = "weight", nullable = false)
	private int weight;

	@Column(name = "wage_rate_applied", nullable = false)
	private int wageRateApplied;

	@Column(name = "net_amount", nullable = false)
	private int netAmount;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "payment_reference", length = 255)
	private String paymentReference;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
