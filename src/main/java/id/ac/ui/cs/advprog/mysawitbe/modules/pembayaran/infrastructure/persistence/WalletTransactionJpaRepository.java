package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionEntity, UUID> {

	List<WalletTransactionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
	boolean existsByReference(String reference);
}
