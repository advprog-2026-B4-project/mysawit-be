package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for wallet read/query operations.
 * A user may only access their own wallet unless they are ADMIN.
 * authentication.name = userId string (set by JwtAuthFilter).
 */
public interface WalletQueryUseCase {

    /**
     * Returns the wallet balance for the given user.
     * User may only fetch their own balance; ADMIN may fetch any.
     */
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    WalletBalanceDTO getUserWalletBalance(UUID userId);

    /**
     * Returns transaction history for the given user wallet.
     * User may only fetch their own history; ADMIN may fetch any.
     */
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    List<WalletTransactionDTO> getWalletTransactions(UUID userId);
}
