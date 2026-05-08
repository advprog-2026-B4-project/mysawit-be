package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletMapperTest {

    private WalletMapper mapper;

    private WalletEntity sampleWalletEntity() {
        return WalletEntity.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .balance(1500000)
                .updatedAt(LocalDateTime.of(2024, 6, 15, 14, 30, 0))
                .build();
    }

    private WalletTransactionEntity sampleTransactionEntity() {
        return WalletTransactionEntity.builder()
                .transactionId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .payrollId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .reference("MID-REF-123")
                .amount(500000)
                .type("CREDIT")
                .createdAt(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
                .build();
    }

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(WalletMapper.class);
    }

    @Test
    void toBalanceDto_mapsEntityToBalanceDto_withUpdatedAtToLastUpdated() {
        WalletEntity entity = sampleWalletEntity();

        WalletBalanceDTO dto = mapper.toBalanceDto(entity);

        assertEquals(entity.getUserId(), dto.userId());
        assertEquals(entity.getBalance(), dto.balance());
        assertEquals(entity.getUpdatedAt(), dto.lastUpdated());
    }

    @Test
    void toBalanceDto_mapsEntityWithZeroBalance() {
        WalletEntity entity = WalletEntity.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .balance(0)
                .updatedAt(LocalDateTime.now())
                .build();

        WalletBalanceDTO dto = mapper.toBalanceDto(entity);

        assertEquals(0, dto.balance());
    }

    @Test
    void toTransactionDto_mapsEntityToTransactionDto() {
        WalletTransactionEntity entity = sampleTransactionEntity();

        WalletTransactionDTO dto = mapper.toTransactionDto(entity);

        assertEquals(entity.getTransactionId(), dto.transactionId());
        assertEquals(entity.getUserId(), dto.userId());
        assertEquals(entity.getPayrollId(), dto.payrollId());
        assertEquals(entity.getAmount(), dto.amount());
        assertEquals(entity.getType(), dto.type());
        assertEquals(entity.getReference(), dto.reference());
        assertEquals(entity.getCreatedAt(), dto.createdAt());
    }

    @Test
    void toTransactionDto_mapsEntityWithNullPayrollId() {
        WalletTransactionEntity entity = sampleTransactionEntity();
        entity.setPayrollId(null);

        WalletTransactionDTO dto = mapper.toTransactionDto(entity);

        assertNull(dto.payrollId());
    }

    @Test
    void toTransactionDto_mapsEntityWithNullReference() {
        WalletTransactionEntity entity = sampleTransactionEntity();
        entity.setReference(null);

        WalletTransactionDTO dto = mapper.toTransactionDto(entity);

        assertNull(dto.reference());
    }

    @Test
    void toTransactionDtoList_mapsListOfEntities() {
        WalletTransactionEntity entity1 = sampleTransactionEntity();
        WalletTransactionEntity entity2 = WalletTransactionEntity.builder()
                .transactionId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .payrollId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .reference("MID-REF-456")
                .amount(-100000)
                .type("DEBIT")
                .createdAt(LocalDateTime.of(2024, 6, 16, 11, 0, 0))
                .build();

        List<WalletTransactionDTO> dtos = mapper.toTransactionDtoList(List.of(entity1, entity2));

        assertEquals(2, dtos.size());
        assertEquals(entity1.getTransactionId(), dtos.get(0).transactionId());
        assertEquals(entity2.getTransactionId(), dtos.get(1).transactionId());
        assertEquals("CREDIT", dtos.get(0).type());
        assertEquals("DEBIT", dtos.get(1).type());
    }

    @Test
    void toTransactionDtoList_returnsEmptyList_whenInputIsEmpty() {
        List<WalletTransactionDTO> dtos = mapper.toTransactionDtoList(List.of());

        assertTrue(dtos.isEmpty());
    }

    @Test
    void toBalanceDto_mapsEntityWithNullUpdatedAt() {
        WalletEntity entity = WalletEntity.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .balance(500000)
                .updatedAt(null)
                .build();

        WalletBalanceDTO dto = mapper.toBalanceDto(entity);

        assertNull(dto.lastUpdated());
        assertEquals(entity.getUserId(), dto.userId());
        assertEquals(entity.getBalance(), dto.balance());
    }

    @Test
    void toBalanceDto_mapsEntityWithMaxBalance() {
        WalletEntity entity = WalletEntity.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .balance(Integer.MAX_VALUE)
                .updatedAt(LocalDateTime.now())
                .build();

        WalletBalanceDTO dto = mapper.toBalanceDto(entity);

        assertEquals(Integer.MAX_VALUE, dto.balance());
    }

    @Test
    void toBalanceDto_returnsNull_whenEntityIsNull() {
        WalletBalanceDTO dto = mapper.toBalanceDto(null);

        assertNull(dto);
    }

    @Test
    void toTransactionDto_returnsNull_whenEntityIsNull() {
        WalletTransactionDTO dto = mapper.toTransactionDto(null);

        assertNull(dto);
    }

    @Test
    void toTransactionDtoList_returnsNull_whenListIsNull() {
        List<WalletTransactionDTO> dtos = mapper.toTransactionDtoList(null);

        assertNull(dtos);
    }
}