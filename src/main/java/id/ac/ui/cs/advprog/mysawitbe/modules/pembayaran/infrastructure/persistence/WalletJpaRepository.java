package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {
}
