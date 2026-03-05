package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CoordinateJpaMapper.class})
public interface KebunJpaMapper {

    KebunDTO toDto(KebunJpaEntity entity);

    /**
     * KebunDTO tidak menyimpan mandorId, jadi kita ignore agar tidak menghapus assignment mandor secara tidak sengaja.
     */
    @Mapping(target = "mandorId", ignore = true)
    KebunJpaEntity toEntity(KebunDTO dto);
}