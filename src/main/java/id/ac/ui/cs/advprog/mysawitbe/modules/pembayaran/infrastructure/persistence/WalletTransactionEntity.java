package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "transaction_id", nullable = false, updatable = false)
	private UUID transactionId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "payroll_id")
	private UUID payrollId;

	@Column(name = "reference", length = 64)
	private String reference;

	@Column(name = "amount", nullable = false)
	private int amount;

	@Column(name = "type", nullable = false, length = 32)
	private String type;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
