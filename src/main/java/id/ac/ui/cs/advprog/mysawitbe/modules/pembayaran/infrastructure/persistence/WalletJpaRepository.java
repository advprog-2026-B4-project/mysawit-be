package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {

    @Query("SELECT COALESCE(SUM(w.balance.amountSmallestUnit), 0) FROM WalletEntity w")
    long sumAllBalances();
}
