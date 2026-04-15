package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;

import java.util.UUID;

/**
 * Use case interface for panen write operations.
 */
public interface PanenCommandUseCase {

    /**
     * Record a single harvest entry. One per buruh per day; enforced at use-case level.
     * Weight in grams. photoUrls are R2 storage URLs.
     */
    PanenDTO createPanen(UUID buruhId, String description, int weight, java.util.List<String> photoUrls);

    /**
     * Mandor approves a harvest record.
     * Publishes PanenApprovedEvent (triggers async payroll creation).
     */
    PanenDTO approvePanen(UUID panenId, UUID mandorId);

    /**
     * Mandor rejects a harvest record with a reason.
     * Publishes PanenRejectedEvent.
     */
    PanenDTO rejectPanen(UUID panenId, UUID mandorId, String reason);
}
