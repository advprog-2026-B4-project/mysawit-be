package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Memastikan tidak memuat Spring Context
class PanenQueryImplTest {

    @Mock
    private PanenRepositoryPort repositoryPort;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Mock
    private KebunQueryUseCase kebunQueryUseCase;

    @InjectMocks
    private PanenQueryImpl panenQuery;

    private UUID panenId;
    private UUID buruhId;
    private UUID mandorId;
    private UUID kebunId;
    private PanenDTO samplePanen;
    private UserDTO sampleBuruh;

    @BeforeEach
    void setUp() {
        panenId = UUID.randomUUID();
        buruhId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        kebunId = UUID.randomUUID();

        // Asumsi struktur DTO menggunakan Record sesuai yang ada di impl
        samplePanen = new PanenDTO(
                panenId, buruhId, "Nama Buruh", kebunId, "Deskripsi", 50,
                "PENDING", "", List.of(), LocalDateTime.now()
        );

        sampleBuruh = new UserDTO(buruhId, "budi123", "Budi (Buruh)", "BURUH", "budi@test.com");
    }

    // ==========================================
    // Test: getPanenById
    // ==========================================
    @Test
    void getPanenById_Success() {
        when(repositoryPort.findById(panenId)).thenReturn(samplePanen);

        PanenDTO result = panenQuery.getPanenById(panenId);

        assertNotNull(result);
        assertEquals(panenId, result.panenId());
        verify(repositoryPort, times(1)).findById(panenId);
    }

    @Test
    void getPanenById_NotFound_ThrowsException() {
        when(repositoryPort.findById(panenId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> panenQuery.getPanenById(panenId));

        assertEquals("Laporan panen tidak ditemukan.", exception.getMessage());
    }

    // ==========================================
    // Test: getApprovedPanenByKebun
    // ==========================================
    @Test
    void getApprovedPanenByKebun_Success() {
        when(repositoryPort.findByKebunIdAndStatus(kebunId, "APPROVED")).thenReturn(List.of(samplePanen));

        List<PanenDTO> result = panenQuery.getApprovedPanenByKebun(kebunId);

        assertEquals(1, result.size());
        verify(repositoryPort, times(1)).findByKebunIdAndStatus(kebunId, "APPROVED");
    }

    // ==========================================
    // Test: listPanenByBuruh
    // ==========================================
    @Test
    void listPanenByBuruh_Success() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh);
        when(repositoryPort.findByBuruhId(buruhId, start, end, "PENDING")).thenReturn(List.of(samplePanen));

        List<PanenDTO> result = panenQuery.listPanenByBuruh(buruhId, start, end, "PENDING");

        assertEquals(1, result.size());
        assertEquals("Budi (Buruh)", result.get(0).buruhName()); // Pastikan mapping nama berjalan
    }

    // ==========================================
    // Test: listPanenByMandor
    // ==========================================
    @Test
    void listPanenByMandor_NoKebun_ReturnsEmptyList() {
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(null);

        List<PanenDTO> result = panenQuery.listPanenByMandor(mandorId, "Budi", LocalDate.now());

        assertTrue(result.isEmpty());
        verify(repositoryPort, never()).findByKebunIdAndDate(any(), any());
    }

    @Test
    void listPanenByMandor_WithKebun_MatchesSearchName() {
        LocalDate date = LocalDate.now();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(repositoryPort.findByKebunIdAndDate(kebunId, date)).thenReturn(List.of(samplePanen));
        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh); // Nama: "Budi (Buruh)"

        // Filter nama "budi" (case insensitive)
        List<PanenDTO> result = panenQuery.listPanenByMandor(mandorId, "budi", date);

        assertEquals(1, result.size());
        assertEquals("Budi (Buruh)", result.get(0).buruhName());
    }

    @Test
    void listPanenByMandor_WithKebun_DoesNotMatchSearchName() {
        LocalDate date = LocalDate.now();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(repositoryPort.findByKebunIdAndDate(kebunId, date)).thenReturn(List.of(samplePanen));
        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh); // Nama: "Budi (Buruh)"

        // Mencari nama yang tidak ada
        List<PanenDTO> result = panenQuery.listPanenByMandor(mandorId, "Agus", date);

        assertTrue(result.isEmpty());
    }

    // ==========================================
    // Test: hasPanenToday
    // ==========================================
    @Test
    void hasPanenToday_NullBuruhId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> panenQuery.hasPanenToday(null, LocalDate.now()));
    }

    @Test
    void hasPanenToday_NullDate_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> panenQuery.hasPanenToday(buruhId, null));
    }

    @Test
    void hasPanenToday_Success() {
        LocalDate date = LocalDate.now();
        when(repositoryPort.existsByBuruhIdAndDate(buruhId, date)).thenReturn(true);

        boolean result = panenQuery.hasPanenToday(buruhId, date);

        assertTrue(result);
    }

    // ==========================================
    // Test: listPanenByBuruhWithAuth
    // ==========================================
    @Test
    void listPanenByBuruhWithAuth_BuruhNotFound_ThrowsException() {
        when(userQueryUseCase.getUserById(buruhId)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> 
            panenQuery.listPanenByBuruhWithAuth(buruhId, buruhId, LocalDate.now(), LocalDate.now(), "PENDING")
        );
    }

    @Test
    void listPanenByBuruhWithAuth_OwnData_Success() throws IllegalAccessException {
        LocalDate date = LocalDate.now();
        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh);
        when(repositoryPort.findByBuruhId(buruhId, date, date, "PENDING")).thenReturn(List.of(samplePanen));

        // requesterId == buruhId
        List<PanenDTO> result = panenQuery.listPanenByBuruhWithAuth(buruhId, buruhId, date, date, "PENDING");

        assertEquals(1, result.size());
    }

    @Test
    void listPanenByBuruhWithAuth_MandorSupervise_Success() throws IllegalAccessException {
        LocalDate date = LocalDate.now();
        UserDTO mandor = new UserDTO(mandorId, "mandor_a", "Mandor A", "MANDOR", "mandor@test.com");
        
        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh);
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandor);
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(List.of(sampleBuruh)); // Buruh ada di bawah mandor ini
        when(repositoryPort.findByBuruhId(buruhId, date, date, "PENDING")).thenReturn(List.of(samplePanen));

        List<PanenDTO> result = panenQuery.listPanenByBuruhWithAuth(buruhId, mandorId, date, date, "PENDING");

        assertEquals(1, result.size());
    }

    @Test
    void listPanenByBuruhWithAuth_UnauthorizedAccess_ThrowsException() {
        UUID randomUserId = UUID.randomUUID();
        UserDTO randomUser = new UserDTO(randomUserId, "random_user", "Random", "BURUH", "random@test.com");

        when(userQueryUseCase.getUserById(buruhId)).thenReturn(sampleBuruh);
        when(userQueryUseCase.getUserById(randomUserId)).thenReturn(randomUser); // Bukan mandor, bukan buruh itu sendiri

        assertThrows(IllegalAccessException.class, () ->
                panenQuery.listPanenByBuruhWithAuth(buruhId, randomUserId, LocalDate.now(), LocalDate.now(), "PENDING")
        );
    }
}