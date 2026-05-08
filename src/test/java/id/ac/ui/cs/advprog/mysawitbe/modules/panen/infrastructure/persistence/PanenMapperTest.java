package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenPhoto;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;

class PanenMapperTest {

    private PanenMapper mapper;

    private UUID panenId;
    private UUID buruhId;
    private UUID kebunId;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        // Instansiasi langsung dari class hasil generate MapStruct (Tanpa Spring Context)
        mapper = new PanenMapperImpl();

        panenId = UUID.randomUUID();
        buruhId = UUID.randomUUID();
        kebunId = UUID.randomUUID();
        timestamp = LocalDateTime.now();
    }

    // =========================================================================
    // 1. Domain -> Entity (toEntity)
    // =========================================================================
    @Nested
    class ToEntityTests {

        @Test
        void shouldMapDomainToEntityWithPhotos() {
            PanenPhoto photo = new PanenPhoto(UUID.randomUUID(), "http://photo.com/1.jpg");
            Panen domain = new Panen(panenId, buruhId, "Budi", kebunId, "Desc", 100,
                    PanenStatus.PENDING, null, timestamp, List.of(photo));

            PanenEntity entity = mapper.toEntity(domain);

            assertNotNull(entity);
            assertEquals(panenId, entity.getPanenId());
            assertEquals(100, entity.getWeight());
            assertEquals(timestamp, entity.getCreatedAt());
            assertEquals(timestamp.toLocalDate(), entity.getHarvestDate());
            assertEquals(PanenStatus.PENDING, entity.getStatus());

            // Verifikasi custom AfterMapping: linkPhotosToEntity
            assertNotNull(entity.getPhotos());
            assertEquals(1, entity.getPhotos().size());
            assertEquals("http://photo.com/1.jpg", entity.getPhotos().get(0).getPhotoUrl());
            assertEquals(entity, entity.getPhotos().get(0).getHarvestReport()); // Bidirectional link
        }

        @Test
        void shouldMapDomainToEntityWithNullPhotos() {
            // Test branch: if (panen.getPhotos() == null) return;
            Panen domain = new Panen(panenId, buruhId, "Budi", kebunId, "Desc", 100,
                    PanenStatus.PENDING, null, timestamp, null);

            PanenEntity entity = mapper.toEntity(domain);

            assertNotNull(entity);
            assertTrue(entity.getPhotos() == null || entity.getPhotos().isEmpty());
        }

        @Test
        void shouldReturnNullWhenDomainIsNull() {
            assertNull(mapper.toEntity(null));
        }
    }

    // =========================================================================
    // 2. Entity -> Domain (toDomain)
    // =========================================================================
    @Nested
    class ToDomainTests {

        @Test
        void shouldMapEntityToDomainWithPhotos() {
            PanenEntity entity = new PanenEntity();
            entity.setPanenId(panenId);
            entity.setBuruhId(buruhId);
            entity.setKebunId(kebunId);
            entity.setWeight(150);
            entity.setStatus(PanenStatus.APPROVED);
            entity.setRejectionReason(null);
            entity.setCreatedAt(timestamp);
            entity.setHarvestDate(timestamp.toLocalDate());

            PanenPhotoEntity photoEntity = new PanenPhotoEntity();
            photoEntity.setId(UUID.randomUUID());
            photoEntity.setPhotoUrl("http://photo.com/2.jpg");
            entity.setPhotos(List.of(photoEntity));

            Panen domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(panenId, domain.getPanenId());
            assertEquals(150, domain.getWeight());
            assertEquals(PanenStatus.APPROVED, domain.getStatus());
            assertEquals(timestamp, domain.getTimestamp());
            assertNull(domain.getBuruhName()); // Di-ignore di @Mapping

            // Verifikasi custom method: entityPhotosToDomain
            assertNotNull(domain.getPhotos());
            assertEquals(1, domain.getPhotos().size());
            assertEquals("http://photo.com/2.jpg", domain.getPhotos().get(0).getPhotoUrl());
        }

        @Test
        void shouldMapEntityToDomainWithNullPhotos() {
            // Test branch: if (entities == null) return List.of();
            PanenEntity entity = new PanenEntity();
            entity.setCreatedAt(timestamp);
            entity.setPhotos(null);

            Panen domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertNotNull(domain.getPhotos());
            assertTrue(domain.getPhotos().isEmpty()); // Custom mapper membalikkan List.of() saat null
        }

        @Test
        void shouldReturnNullWhenEntityIsNull() {
            assertNull(mapper.toDomain(null));
        }
    }

    // =========================================================================
    // 3. Domain -> DTO (toDTO)
    // =========================================================================
    @Nested
    class ToDTOTests {

        @Test
        void shouldMapDomainToDTOWithPhotos() {
            PanenPhoto photo = new PanenPhoto(UUID.randomUUID(), "http://photo.com/3.jpg");
            Panen domain = new Panen(panenId, buruhId, "Budi", kebunId, "Test DTO", 120,
                    PanenStatus.REJECTED, "Kualitas buruk", timestamp, List.of(photo));

            PanenDTO dto = mapper.toDTO(domain);

            assertNotNull(dto);
            assertEquals(panenId, dto.panenId());
            assertEquals("REJECTED", dto.status()); // Enum -> String
            assertEquals("Kualitas buruk", dto.rejectionReason());

            // Verifikasi custom method: domainPhotosToPhotoDTOs
            assertNotNull(dto.photos());
            assertEquals(1, dto.photos().size());
            assertEquals("http://photo.com/3.jpg", dto.photos().get(0).url());
        }

        @Test
        void shouldMapDomainToDTOWithNullPhotos() {
            // Test branch: if (photos == null) return List.of();
            Panen domain = new Panen(panenId, buruhId, "Budi", kebunId, "Test DTO", 120,
                    PanenStatus.REJECTED, "Kualitas buruk", timestamp, null);

            PanenDTO dto = mapper.toDTO(domain);

            assertNotNull(dto);
            assertNotNull(dto.photos());
            assertTrue(dto.photos().isEmpty());
        }

        @Test
        void shouldReturnNullWhenDomainIsNull() {
            assertNull(mapper.toDTO(null));
        }
    }

    // =========================================================================
    // 4. DTO -> Domain (dtoToDomain)
    // =========================================================================
    @Nested
    class DtoToDomainTests {

        @Test
        void shouldMapDtoToDomainWithPhotos() {
            PanenDTO.PhotoDTO photoDto = new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo.com/4.jpg");
            PanenDTO dto = new PanenDTO(panenId, buruhId, "Budi", kebunId, "Desc", 200,
                    "APPROVED", null, List.of(photoDto), timestamp);

            Panen domain = mapper.dtoToDomain(dto);

            assertNotNull(domain);
            assertEquals(panenId, domain.getPanenId());
            assertEquals(PanenStatus.APPROVED, domain.getStatus()); // String -> Enum

            // Verifikasi custom method: photoDTOsToDomain
            assertNotNull(domain.getPhotos());
            assertEquals(1, domain.getPhotos().size());
            assertEquals("http://photo.com/4.jpg", domain.getPhotos().get(0).getPhotoUrl());
        }

        @Test
        void shouldMapDtoToDomainWithNullPhotos() {
            // Test branch: if (photoDTOs == null) return List.of();
            PanenDTO dto = new PanenDTO(panenId, buruhId, "Budi", kebunId, "Desc", 200,
                    "APPROVED", null, null, timestamp);

            Panen domain = mapper.dtoToDomain(dto);

            assertNotNull(domain);
            assertNotNull(domain.getPhotos());
            assertTrue(domain.getPhotos().isEmpty());
        }

        @Test
        void shouldReturnNullWhenDtoIsNull() {
            assertNull(mapper.dtoToDomain(null));
        }
    }

    // =========================================================================
    // 5. Entity -> DTO (entityToDto)
    // =========================================================================
    @Nested
    class EntityToDtoTests {

        @Test
        void shouldMapEntityToDtoWithPhotos() {
            PanenEntity entity = new PanenEntity();
            entity.setPanenId(panenId);
            entity.setStatus(PanenStatus.PENDING);
            entity.setCreatedAt(timestamp);

            PanenPhotoEntity photoEntity = new PanenPhotoEntity();
            photoEntity.setId(UUID.randomUUID());
            photoEntity.setPhotoUrl("http://photo.com/5.jpg");
            entity.setPhotos(List.of(photoEntity));

            PanenDTO dto = mapper.entityToDto(entity);

            assertNotNull(dto);
            assertEquals(panenId, dto.panenId());
            assertEquals("PENDING", dto.status()); // Enum -> String
            assertEquals(timestamp, dto.timestamp());

            // Verifikasi custom method: entityPhotosToPhotoDTOs
            assertNotNull(dto.photos());
            assertEquals(1, dto.photos().size());
            assertEquals("http://photo.com/5.jpg", dto.photos().get(0).url());
        }

        @Test
        void shouldMapEntityToDtoWithNullPhotos() {
            // Test branch: if (entities == null) return List.of();
            PanenEntity entity = new PanenEntity();
            entity.setStatus(PanenStatus.PENDING);
            entity.setPhotos(null);

            PanenDTO dto = mapper.entityToDto(entity);

            assertNotNull(dto);
            assertNotNull(dto.photos());
            assertTrue(dto.photos().isEmpty());
        }

        @Test
        void shouldReturnNullWhenEntityIsNull() {
            assertNull(mapper.entityToDto(null));
        }
    }
}