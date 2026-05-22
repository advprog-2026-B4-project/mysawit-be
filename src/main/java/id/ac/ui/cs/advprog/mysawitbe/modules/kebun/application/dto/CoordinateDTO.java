package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto;

/**
 * Coordinate pair for kebun boundary polygon.
 * lat/lng stored as integer microdegrees (value * 1_000_000) to avoid floating-point issues.
 */
public record CoordinateDTO(
        int lat,
        int lng
) {}
