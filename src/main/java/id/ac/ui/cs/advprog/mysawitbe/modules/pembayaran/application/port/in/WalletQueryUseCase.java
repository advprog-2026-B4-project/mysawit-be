package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for wallet read/query operations.
 */
public interface WalletQueryUseCase {

    WalletBalanceDTO getUserWalletBalance(UUID userId);

    /**
     * Returns transaction history for a given user wallet.
     */
    List<WalletTransactionDTO> getWalletTransactions(UUID userId);
}
