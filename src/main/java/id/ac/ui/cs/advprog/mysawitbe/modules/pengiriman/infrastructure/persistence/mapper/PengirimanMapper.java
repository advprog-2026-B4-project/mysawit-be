package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanPanenItemJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PengirimanMapper {

    @Mapping(target = "panenIds", source = "panenItems")
    @Mapping(target = "supirName", ignore = true)
    @Mapping(target = "mandorName", ignore = true)
    PengirimanDTO toDto(PengirimanJpaEntity entity);

    @Mapping(target = "panenItems", source = "panenIds")
    PengirimanJpaEntity toEntity(PengirimanDTO dto);

    List<PengirimanDTO> toDtoList(List<PengirimanJpaEntity> entities);

    default List<UUID> mapPanenItems(List<PengirimanPanenItemJpaEntity> panenItems) {
        if (panenItems == null) {
            return List.of();
        }
        return panenItems.stream()
                .map(PengirimanPanenItemJpaEntity::getPanenId)
                .toList();
    }

    default List<PengirimanPanenItemJpaEntity> mapPanenIds(List<UUID> panenIds) {
        if (panenIds == null) {
            return List.of();
        }
        return panenIds.stream()
                .map(panenId -> PengirimanPanenItemJpaEntity.builder().panenId(panenId).build())
                .toList();
    }

    @AfterMapping
    default void attachParent(@MappingTarget PengirimanJpaEntity entity) {
        if (entity.getPanenItems() == null) {
            return;
        }
        entity.getPanenItems().forEach(item -> item.setPengiriman(entity));
    }
}
