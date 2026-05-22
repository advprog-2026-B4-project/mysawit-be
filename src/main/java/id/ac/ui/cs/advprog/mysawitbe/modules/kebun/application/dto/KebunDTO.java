package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Immutable data transfer object for kebun (plantation) data.
 * coordinates: ordered list of boundary polygon vertices.
 */
public record KebunDTO(
        UUID kebunId,
        String nama,
        String kode,
        int luas,
        List<CoordinateDTO> coordinates
) {}
