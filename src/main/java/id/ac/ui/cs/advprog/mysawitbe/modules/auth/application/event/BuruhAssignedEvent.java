package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event;

import java.util.UUID;

/**
 * Published when an admin assigns a buruh to a mandor.
 * Consumed by: kebun module (to update kebun buruh lists), notification module.
 */
public record BuruhAssignedEvent(
        UUID buruhId,
        UUID mandorId
) {}
