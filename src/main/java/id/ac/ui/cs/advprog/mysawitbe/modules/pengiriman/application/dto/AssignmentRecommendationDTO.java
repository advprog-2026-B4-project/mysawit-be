package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Knapsack-based recommendation for one delivery assignment.
 */
public record AssignmentRecommendationDTO(
        List<UUID> panenIds,
        List<AssignablePanenDTO> panenItems,
        int totalWeight,
        int maxCapacity,
        int remainingCapacity
) {}
