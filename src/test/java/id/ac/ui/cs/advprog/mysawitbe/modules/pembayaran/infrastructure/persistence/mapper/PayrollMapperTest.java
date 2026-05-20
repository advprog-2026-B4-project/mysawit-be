package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PayrollMapperTest {

    private PayrollMapper mapper;

    private PayrollEntity sampleEntity() {
        return PayrollEntity.builder()
                .payrollId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .role("PANEN")
                .referenceId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .referenceType("PANEN")
                .weight(50000)
                .wageRateApplied(10)
                .netAmount(500000)
                .status("PENDING")
                .rejectionReason(null)
                .processedAt(LocalDateTime.of(2024, 6, 15, 10, 30))
                .createdAt(LocalDateTime.of(2024, 6, 14, 8, 0))
                .paymentReference("MID-123456")
                .build();
    }

    private PayrollDTO sampleDto() {
        return new PayrollDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "PANEN",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "PANEN",
                50000,
                10,
                500000,
                "PENDING",
                null,
                LocalDateTime.of(2024, 6, 15, 10, 30),
                LocalDateTime.of(2024, 6, 14, 8, 0)
        );
    }

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(PayrollMapper.class);
    }

    @Test
    void toDto_mapsEntityToDto_withEmptyEvidencePhotoUrls() {
        PayrollEntity entity = sampleEntity();

        PayrollDTO dto = mapper.toDto(entity);

        assertEquals(entity.getPayrollId(), dto.payrollId());
        assertEquals(entity.getUserId(), dto.userId());
        assertEquals(entity.getRole(), dto.role());
        assertEquals(entity.getReferenceId(), dto.referenceId());
        assertEquals(entity.getReferenceType(), dto.referenceType());
        assertEquals(entity.getWeight(), dto.weight());
        assertEquals(entity.getWageRateApplied(), dto.wageRateApplied());
        assertEquals(entity.getNetAmount(), dto.netAmount());
        assertEquals(entity.getStatus(), dto.status());
        assertEquals(entity.getRejectionReason(), dto.rejectionReason());
        assertEquals(entity.getProcessedAt(), dto.processedAt());
        assertEquals(entity.getCreatedAt(), dto.createdAt());
        assertNotNull(dto.evidencePhotoUrls());
        assertTrue(dto.evidencePhotoUrls().isEmpty());
    }

    @Test
    void toDto_setsEmptyList_whenEvidencePhotoUrlsIsNullInEntity() {
        PayrollEntity entity = sampleEntity();
        entity.setPaymentReference(null);

        PayrollDTO dto = mapper.toDto(entity);

        assertNotNull(dto.evidencePhotoUrls());
        assertTrue(dto.evidencePhotoUrls().isEmpty());
    }

    @Test
    void toEntity_mapsDtoToEntity_ignoresPaymentReference() {
        PayrollDTO dto = sampleDto();

        PayrollEntity entity = mapper.toEntity(dto);

        assertEquals(dto.payrollId(), entity.getPayrollId());
        assertEquals(dto.userId(), entity.getUserId());
        assertEquals(dto.role(), entity.getRole());
        assertEquals(dto.referenceId(), entity.getReferenceId());
        assertEquals(dto.referenceType(), entity.getReferenceType());
        assertEquals(dto.weight(), entity.getWeight());
        assertEquals(dto.wageRateApplied(), entity.getWageRateApplied());
        assertEquals(dto.netAmount(), entity.getNetAmount());
        assertEquals(dto.status(), entity.getStatus());
        assertEquals(dto.rejectionReason(), entity.getRejectionReason());
        assertEquals(dto.processedAt(), entity.getProcessedAt());
        assertEquals(dto.createdAt(), entity.getCreatedAt());
    }

    @Test
    void toDtoList_mapsListOfEntities() {
        PayrollEntity entity1 = sampleEntity();
        PayrollEntity entity2 = PayrollEntity.builder()
                .payrollId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .userId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .role("PENGIRIMAN")
                .referenceId(UUID.fromString("66666666-6666-6666-6666-666666666666"))
                .referenceType("PENGIRIMAN")
                .weight(30000)
                .wageRateApplied(15)
                .netAmount(450000)
                .status("APPROVED")
                .rejectionReason(null)
                .processedAt(LocalDateTime.of(2024, 6, 16, 12, 0))
                .createdAt(LocalDateTime.of(2024, 6, 15, 9, 0))
                .paymentReference("MID-789012")
                .build();

        List<PayrollDTO> dtos = mapper.toDtoList(List.of(entity1, entity2));

        assertEquals(2, dtos.size());
        assertEquals(entity1.getPayrollId(), dtos.get(0).payrollId());
        assertEquals(entity2.getPayrollId(), dtos.get(1).payrollId());
        assertTrue(dtos.get(0).evidencePhotoUrls().isEmpty());
        assertTrue(dtos.get(1).evidencePhotoUrls().isEmpty());
    }

    @Test
    void toDtoList_returnsEmptyList_whenInputIsEmpty() {
        List<PayrollDTO> dtos = mapper.toDtoList(List.of());

        assertTrue(dtos.isEmpty());
    }

    @Test
    void toStatusDto_mapsEntityToStatusDto() {
        PayrollEntity entity = sampleEntity();

        PayrollStatusDTO statusDto = mapper.toStatusDto(entity);

        assertEquals(entity.getPayrollId(), statusDto.payrollId());
        assertEquals(entity.getUserId(), statusDto.userId());
        assertEquals(entity.getNetAmount(), statusDto.amount());
        assertEquals(entity.getStatus(), statusDto.status());
        assertEquals(entity.getProcessedAt(), statusDto.processedAt());
        assertEquals(entity.getPaymentReference(), statusDto.paymentReference());
    }

    @Test
    void toStatusDto_mapsEntityWithNullProcessedAt() {
        PayrollEntity entity = sampleEntity();
        entity.setProcessedAt(null);

        PayrollStatusDTO statusDto = mapper.toStatusDto(entity);

        assertNull(statusDto.processedAt());
    }

    @Test
    void toDto_returnsNull_whenEntityIsNull() {
        PayrollDTO dto = mapper.toDto(null);

        assertNull(dto);
    }

    @Test
    void toEntity_returnsNull_whenDtoIsNull() {
        PayrollEntity entity = mapper.toEntity(null);

        assertNull(entity);
    }

    @Test
    void toDtoList_returnsNull_whenEntitiesIsNull() {
        List<PayrollDTO> dtos = mapper.toDtoList(null);

        assertNull(dtos);
    }
}