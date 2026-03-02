package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event;

import java.util.UUID;

/**
 * Published when an admin assigns a mandor to a kebun.
 * Consumed by: notification module.
 */
public record MandorAssignedToKebunEvent(
        UUID mandorId,
        UUID kebunId
) {}
