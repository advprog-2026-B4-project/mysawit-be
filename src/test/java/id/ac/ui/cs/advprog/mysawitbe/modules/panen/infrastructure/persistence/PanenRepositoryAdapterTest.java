package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanenRepositoryAdapterTest {

    @Mock
    private PanenJpaRepository jpaRepository;

    @Mock
    private PanenMapper mapper;

    @InjectMocks
    private PanenRepositoryAdapter adapter;

    private UUID panenId;
    private UUID kebunId;
    private UUID buruhId;
    private PanenDTO sampleDto;
    private Panen sampleDomain;
    private PanenEntity sampleEntity;

    @BeforeEach
    void setUp() {
        panenId = UUID.randomUUID();
        kebunId = UUID.randomUUID();
        buruhId = UUID.randomUUID();

        sampleDto = new PanenDTO(panenId, buruhId, "Budi", kebunId, "Test", 100, "PENDING", null, List.of(), LocalDateTime.now());
        
        sampleDomain = new Panen(
                panenId, 
                buruhId, 
                "Budi", 
                kebunId, 
                "Test", 
                100, 
                PanenStatus.PENDING, 
                null, 
                LocalDateTime.now(), 
                List.of()
        );
        
        // Entity initialization
        sampleEntity = new PanenEntity();
        sampleEntity.setPanenId(panenId);
    }

    @Test
    void save_Success() {
        when(mapper.dtoToDomain(sampleDto)).thenReturn(sampleDomain);
        when(mapper.toEntity(sampleDomain)).thenReturn(sampleEntity);
        when(jpaRepository.save(sampleEntity)).thenReturn(sampleEntity);
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        PanenDTO result = adapter.save(sampleDto);

        assertNotNull(result);
        verify(jpaRepository).save(sampleEntity);
    }

    @Test
    void findById_Exists_ReturnsDto() {
        when(jpaRepository.findByIdWithPhotos(panenId)).thenReturn(Optional.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        PanenDTO result = adapter.findById(panenId);

        assertNotNull(result);
        assertEquals(panenId, result.panenId());
    }

    @Test
    void findById_NotExists_ReturnsNull() {
        when(jpaRepository.findByIdWithPhotos(panenId)).thenReturn(Optional.empty());

        PanenDTO result = adapter.findById(panenId);

        assertNull(result); // Harus mengembalikan null, jangan throw error (sesuai README)
    }

    @Test
    void existsByBuruhIdAndDate_ReturnsBoolean() {
        LocalDate date = LocalDate.now();
        when(jpaRepository.existsByBuruhIdAndHarvestDate(buruhId, date)).thenReturn(true);

        boolean result = adapter.existsByBuruhIdAndDate(buruhId, date);

        assertTrue(result);
    }

    @Test
    void findByKebunIdAndStatus_ValidStatus_Success() {
        when(jpaRepository.findByKebunIdAndStatus(kebunId, PanenStatus.APPROVED)).thenReturn(List.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        List<PanenDTO> result = adapter.findByKebunIdAndStatus(kebunId, "APPROVED");

        assertEquals(1, result.size());
        verify(jpaRepository).findByKebunIdAndStatus(kebunId, PanenStatus.APPROVED);
    }

    @Test
    void findByBuruhId_NullStatus_MapsToNullEnum() {
        LocalDate date = LocalDate.now();
        when(jpaRepository.findByBuruhIdWithFilters(buruhId, date, date, null)).thenReturn(List.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        List<PanenDTO> result = adapter.findByBuruhId(buruhId, date, date, null);

        assertEquals(1, result.size());
        verify(jpaRepository).findByBuruhIdWithFilters(buruhId, date, date, null);
    }

    @Test
    void findByBuruhId_ValidStatus_MapsToEnum() {
        LocalDate date = LocalDate.now();
        when(jpaRepository.findByBuruhIdWithFilters(buruhId, date, date, PanenStatus.PENDING)).thenReturn(List.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        List<PanenDTO> result = adapter.findByBuruhId(buruhId, date, date, "PENDING");

        assertEquals(1, result.size());
        verify(jpaRepository).findByBuruhIdWithFilters(buruhId, date, date, PanenStatus.PENDING);
    }

    @Test
    void findByBuruhId_BlankStatus_MapsToNullEnum() {
        LocalDate date = LocalDate.now();
        when(jpaRepository.findByBuruhIdWithFilters(buruhId, date, date, null)).thenReturn(List.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        List<PanenDTO> result = adapter.findByBuruhId(buruhId, date, date, "   "); // Cek kondisi status.isBlank()

        assertEquals(1, result.size());
        verify(jpaRepository).findByBuruhIdWithFilters(buruhId, date, date, null);
    }

    @Test
    void findByKebunIdAndDate_Success() {
        LocalDate date = LocalDate.now();
        when(jpaRepository.findByKebunIdAndDateFilter(kebunId, date)).thenReturn(List.of(sampleEntity));
        when(mapper.entityToDto(sampleEntity)).thenReturn(sampleDto);

        List<PanenDTO> result = adapter.findByKebunIdAndDate(kebunId, date);

        assertEquals(1, result.size());
        verify(jpaRepository).findByKebunIdAndDateFilter(kebunId, date);
    }
}