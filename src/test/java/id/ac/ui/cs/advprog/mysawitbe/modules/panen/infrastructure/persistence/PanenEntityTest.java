package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PanenEntityTest {

    private PanenEntity panenEntity;
    private PanenPhotoEntity photoEntity;

    @BeforeEach
    void setUp() {
        panenEntity = PanenEntity.builder()
                .panenId(UUID.randomUUID())
                .buruhId(UUID.randomUUID())
                .kebunId(UUID.randomUUID())
                .weight(100)
                .status(PanenStatus.PENDING)
                .harvestDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        photoEntity = PanenPhotoEntity.builder()
                .id(UUID.randomUUID())
                .photoUrl("http://example.com/photo.jpg")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testAddPhoto_ShouldAddToListAndSetParentReference() {
        panenEntity.addPhoto(photoEntity);

        assertEquals(1, panenEntity.getPhotos().size());
        assertTrue(panenEntity.getPhotos().contains(photoEntity));
        assertEquals(panenEntity, photoEntity.getHarvestReport());
    }

    @Test
    void testRemovePhoto_ShouldRemoveFromListAndNullifyParentReference() {
        panenEntity.addPhoto(photoEntity); // Tambahkan dulu
        
        panenEntity.removePhoto(photoEntity); // Kemudian hapus

        assertEquals(0, panenEntity.getPhotos().size());
        assertFalse(panenEntity.getPhotos().contains(photoEntity));
        assertNull(photoEntity.getHarvestReport());
    }
}