package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;

/**
 * Simple static mapper between {@link VariabelPokokEntity} and {@link VariabelPokokDTO}.
 * Static utility - no instantiation needed.
 */
public final class VariabelPokokMapper {

    private VariabelPokokMapper() {}

    public static VariabelPokokDTO toDto(VariabelPokokEntity entity) {
        return new VariabelPokokDTO(
                entity.getKey(),
                entity.getKey().getLabel(),
                entity.getKey().getDescription(),
                entity.getValue()
        );
    }
}
