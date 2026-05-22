package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CoordinateJpaMapper {
    CoordinateDTO toDto(CoordinateEmbeddable embeddable);
    CoordinateEmbeddable toEmbeddable(CoordinateDTO dto);
}