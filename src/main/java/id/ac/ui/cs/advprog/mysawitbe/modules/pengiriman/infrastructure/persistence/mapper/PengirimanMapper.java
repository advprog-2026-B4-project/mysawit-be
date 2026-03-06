package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PengirimanMapper {

    PengirimanDTO toDto(PengirimanJpaEntity entity);

    PengirimanJpaEntity toEntity(PengirimanDTO dto);

    List<PengirimanDTO> toDtoList(List<PengirimanJpaEntity> entities);
}
