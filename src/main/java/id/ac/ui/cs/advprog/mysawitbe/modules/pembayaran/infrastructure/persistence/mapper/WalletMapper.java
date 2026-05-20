package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(source = "updatedAt", target = "lastUpdated")
    WalletBalanceDTO toBalanceDto(WalletEntity entity);

    WalletTransactionDTO toTransactionDto(WalletTransactionEntity entity);

    List<WalletTransactionDTO> toTransactionDtoList(List<WalletTransactionEntity> entities);
}