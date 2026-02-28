package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable data transfer object for wallet transaction records.
 * amount: in smallest currency unit.
 * type: e.g. CREDIT, DEBIT.
 */
public record WalletTransactionDTO(
        UUID transactionId,
        UUID userId,
        UUID payrollId,
        int amount,
        String type,
        LocalDateTime createdAt
) {}
