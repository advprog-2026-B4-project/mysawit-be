package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenMapperPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenPhoto;

/**
 * MapStruct mapper for conversions between Panen domain model,
 * PanenEntity (JPA), and PanenDTO.
 *
 * Lives in infrastructure/persistence per agent.md architecture rules.
 * Extends PanenMapperPort so application layer can depend on the port abstraction.
 */
@Mapper(componentModel = "spring")
public interface PanenMapper extends PanenMapperPort {

    // ── Domain → Entity ─────────────────────────────────────────────

    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "harvestDate", expression = "java(panen.getTimestamp().toLocalDate())")
    @Mapping(target = "photos", ignore = true)
    PanenEntity toEntity(Panen panen);

    @AfterMapping
    default void linkPhotosToEntity(Panen panen, @MappingTarget PanenEntity entity) {
        if (panen.getPhotos() == null) return;
        for (PanenPhoto photo : panen.getPhotos()) {
            PanenPhotoEntity photoEntity = PanenPhotoEntity.builder()
                    .id(photo.getId())
                    .photoUrl(photo.getPhotoUrl())
                    .createdAt(LocalDateTime.now())
                    .build();
            entity.addPhoto(photoEntity);
        }
    }

    // ── Entity → Domain ─────────────────────────────────────────────

    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "buruhName", ignore = true)
    @Mapping(target = "photos", source = "photos", qualifiedByName = "entityPhotosToDomain")
    Panen toDomain(PanenEntity entity);

    // ── Domain → DTO ───────────────────────────────────────────────

    @Override
    @Mapping(target = "status", expression = "java(panen.getStatus().name())")
    @Mapping(target = "photos", source = "photos", qualifiedByName = "domainPhotosToPhotoDTOs")
    PanenDTO toDTO(Panen panen);

    // ── DTO → Domain ────────────────────────────────────────────────

    @Override
    @Mapping(target = "status", expression = "java(id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus.valueOf(dto.status()))")
    @Mapping(target = "photos", source = "photos", qualifiedByName = "photoDTOsToDomain")
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "timestamp", source = "timestamp")
    Panen dtoToDomain(PanenDTO dto);

    // ── Entity → DTO ────────────────────────────────────────────────

    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "buruhName", ignore = true)
    @Mapping(target = "photos", source = "photos", qualifiedByName = "entityPhotosToPhotoDTOs")
    PanenDTO entityToDto(PanenEntity entity);

    // ── Custom collection mappings ──────────────────────────────────

    @Named("entityPhotosToDomain")
    default List<PanenPhoto> entityPhotosToDomain(List<PanenPhotoEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(e -> new PanenPhoto(e.getId(), e.getPhotoUrl()))
                .toList();
    }

    @Named("domainPhotosToPhotoDTOs")
    default List<PanenDTO.PhotoDTO> domainPhotosToPhotoDTOs(List<PanenPhoto> photos) {
        if (photos == null) return List.of();
        return photos.stream()
                .map(p -> new PanenDTO.PhotoDTO(p.getId(), p.getPhotoUrl()))
                .toList();
    }

    @Named("photoDTOsToDomain")
    default List<PanenPhoto> photoDTOsToDomain(List<PanenDTO.PhotoDTO> photoDTOs) {
        if (photoDTOs == null) return List.of();
        return photoDTOs.stream()
                .map(dto -> new PanenPhoto(dto.photoId(), dto.url()))
                .toList();
    }

    @Named("entityPhotosToPhotoDTOs")
    default List<PanenDTO.PhotoDTO> entityPhotosToPhotoDTOs(List<PanenPhotoEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(e -> new PanenDTO.PhotoDTO(e.getId(), e.getPhotoUrl()))
                .toList();
    }
}
