package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.User;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // JPA Entity -> Domain
    @Mapping(target = "hashedPassword", source = "password")
    @Mapping(target = "role", expression = "java(id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole.valueOf(entity.getRole()))")
    User toDomain(UserJpaEntity entity);

    // Domain -> JPA Entity
    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "role", expression = "java(domain.getRole().name())")
    UserJpaEntity toEntity(User domain);

    // Domain -> DTO
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "role", expression = "java(domain.getRole().name())")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "mandorCertificationNumber", source = "mandorCertificationNumber")
    UserDTO toDTO(User domain);

    // DTO -> Domain (password set separately)
    @Mapping(target = "hashedPassword", ignore = true)
    @Mapping(target = "role", expression = "java(id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole.valueOf(dto.role()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "mandorId", ignore = true)
    User toDomain(UserDTO dto);
}
