package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    PayrollDTO toDto(PayrollEntity entity);

    @Mapping(target = "paymentReference", ignore = true)
    PayrollEntity toEntity(PayrollDTO dto);

    List<PayrollDTO> toDtoList(List<PayrollEntity> entities);

    default PayrollStatusDTO toStatusDto(PayrollEntity entity) {
        return new PayrollStatusDTO(
                entity.getPayrollId(),
                entity.getUserId(),
                entity.getNetAmount(),
                entity.getStatus(),
                entity.getProcessedAt(),
                entity.getPaymentReference()
        );
    }
}