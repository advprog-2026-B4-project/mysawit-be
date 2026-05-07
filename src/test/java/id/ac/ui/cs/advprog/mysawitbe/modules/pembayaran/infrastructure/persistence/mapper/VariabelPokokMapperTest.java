package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VariabelPokokMapperTest {

    @Test
    void toDto_mapsEntityToDto_withKeyLabelAndDescription() {
        VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 5000);

        VariabelPokokDTO dto = VariabelPokokMapper.toDto(entity);

        assertEquals(VariableKey.UPAH_BURUH, dto.key());
        assertEquals("Upah Buruh per Kg", dto.label());
        assertEquals("Upah Buruh untuk setiap kilogram sawit yang dipanen", dto.description());
        assertEquals(5000, dto.value());
    }

    @Test
    void toDto_mapsUpaSupir_withCorrectLabelAndDescription() {
        VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_SUPIR, 7500);

        VariabelPokokDTO dto = VariabelPokokMapper.toDto(entity);

        assertEquals(VariableKey.UPAH_SUPIR, dto.key());
        assertEquals("Upah Supir Truk per Kg", dto.label());
        assertEquals("Upah Supir untuk setiap kilogram sawit yang dibawa", dto.description());
        assertEquals(7500, dto.value());
    }

    @Test
    void toDto_mapsUpaMandor_withCorrectLabelAndDescription() {
        VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_MANDOR, 2000);

        VariabelPokokDTO dto = VariabelPokokMapper.toDto(entity);

        assertEquals(VariableKey.UPAH_MANDOR, dto.key());
        assertEquals("Upah Mandor per Kg", dto.label());
        assertEquals("Upah Mandor untuk setiap kilogram sawit yang diterima pabrik produksi", dto.description());
        assertEquals(2000, dto.value());
    }

    @Test
    void toDto_mapsZeroValue() {
        VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 0);

        VariabelPokokDTO dto = VariabelPokokMapper.toDto(entity);

        assertEquals(0, dto.value());
    }

    @Test
    void toDto_isStateless_doesNotModifyEntity() {
        VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 5000);

        VariabelPokokDTO dto1 = VariabelPokokMapper.toDto(entity);
        VariabelPokokDTO dto2 = VariabelPokokMapper.toDto(entity);

        assertEquals(dto1, dto2);
    }
}