package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.VariabelPokokRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.VariabelPokokMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that fulfils {@link VariabelPokokRepositoryPort} using JPA.
 * Infrastructure concern - the application layer depends only on the port interface.
 */
@Component
@RequiredArgsConstructor
public class VariabelPokokRepositoryAdapter implements VariabelPokokRepositoryPort {

    private final VariabelPokokJpaRepository jpaRepository;

    @Override
    public List<VariabelPokokDTO> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(VariabelPokokMapper::toDto)
                .toList();
    }

    @Override
    public Optional<VariabelPokokDTO> findByKey(VariableKey key) {
        return jpaRepository.findById(key)
                .map(VariabelPokokMapper::toDto);
    }

    @Override
    public VariabelPokokDTO save(VariableKey key, int newValue) {
        VariabelPokokEntity entity = jpaRepository.findById(key)
                .orElseGet(() -> new VariabelPokokEntity(key, newValue));
        entity.setValue(newValue);
        return VariabelPokokMapper.toDto(jpaRepository.save(entity));
    }
}
