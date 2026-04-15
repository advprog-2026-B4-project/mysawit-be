package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for wallet persistence.
 * Implemented by infrastructure/persistence/WalletJpaAdapter.
 */
public interface WalletRepositoryPort {

    WalletBalanceDTO findBalanceByUserId(UUID userId);

    /**
     * Credit the wallet by amount. Returns the updated balance.
     */
    WalletBalanceDTO credit(UUID userId, int amount, UUID payrollId);

    /**
     * Debit the wallet by amount. Returns the updated balance.
     * Throws IllegalStateException if balance is insufficient.
     */
    WalletBalanceDTO debit(UUID userId, int amount, UUID payrollId);

    List<WalletTransactionDTO> findTransactionsByUserId(UUID userId);
}
