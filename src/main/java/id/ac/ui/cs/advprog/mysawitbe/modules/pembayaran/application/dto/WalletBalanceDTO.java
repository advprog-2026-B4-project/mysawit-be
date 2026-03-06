package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of a user wallet balance.
 * balance: in smallest currency unit (e.g. IDR).
 */
public record WalletBalanceDTO(
        UUID userId,
        int balance,
        LocalDateTime lastUpdated
) {}
