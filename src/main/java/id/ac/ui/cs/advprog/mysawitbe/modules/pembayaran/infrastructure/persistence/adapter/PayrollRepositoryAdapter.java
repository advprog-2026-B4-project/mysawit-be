package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PayrollRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.PayrollMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayrollRepositoryAdapter implements PayrollRepositoryPort {

	private final PayrollJpaRepository payrollJpaRepository;
	private final VariabelPokokJpaRepository variabelPokokJpaRepository;
	private final PayrollMapper payrollMapper;

	@Override
	public PayrollDTO save(PayrollDTO payrollDTO) {
		PayrollEntity saved = payrollJpaRepository.save(payrollMapper.toEntity(payrollDTO));
		return payrollMapper.toDto(saved);
	}

	@Override
	public PayrollDTO findById(UUID payrollId) {
		return payrollJpaRepository.findById(payrollId)
				.map(payrollMapper::toDto)
				.orElse(null);
	}

	@Override
	public PayrollStatusDTO findStatusById(UUID payrollId) {
		return payrollJpaRepository.findById(payrollId)
				.map(payrollMapper::toStatusDto)
				.orElse(null);
	}

	@Override
	public PayrollPageDTO findByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status, int page, int size) {
		Specification<PayrollEntity> spec = buildFilter(userId, startDate, endDate, status);
		Page<PayrollEntity> payrollPage = payrollJpaRepository.findAll(spec, toPageable(page, size));
		return toPayrollPage(payrollPage);
	}

	@Override
	public PayrollPageDTO findAll(LocalDate startDate, LocalDate endDate, String status, int page, int size) {
		Specification<PayrollEntity> spec = buildFilter(null, startDate, endDate, status);
		Page<PayrollEntity> payrollPage = payrollJpaRepository.findAll(spec, toPageable(page, size));
		return toPayrollPage(payrollPage);
	}

	@Override
	public boolean existsByUserIdAndRoleAndReferenceIdAndReferenceType(
			UUID userId,
			String role,
			UUID referenceId,
			String referenceType
	) {
		return payrollJpaRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
				userId,
				role,
				referenceId,
				referenceType
		);
	}

	@Override
	public int getWageRate(String role) {
		VariableKey key = toVariableKey(role);
		return variabelPokokJpaRepository.findById(key)
				.map(VariabelPokokEntity::getValue)
				.orElseThrow(() -> new EntityNotFoundException("Variabel pokok not found: " + key));
	}

	@Override
	public void updateWageRate(String role, int newRatePerGram) {
		VariableKey key = toVariableKey(role);
		VariabelPokokEntity entity = variabelPokokJpaRepository.findById(key)
				.orElseThrow(() -> new EntityNotFoundException("Variabel pokok not found: " + key));
		entity.setValue(newRatePerGram);
		variabelPokokJpaRepository.save(entity);
	}

	private Specification<PayrollEntity> buildFilter(
			UUID userId,
			LocalDate startDate,
			LocalDate endDate,
			String status
	) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (userId != null) {
				predicates.add(builder.equal(root.get("userId"), userId));
			}

			if (startDate != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
			}

			if (endDate != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), toEndOfDay(endDate)));
			}

			if (status != null && !status.isBlank()) {
				predicates.add(builder.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
			}

			return builder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private Pageable toPageable(int page, int size) {
		int sanitizedPage = Math.max(page, 0);
		int sanitizedSize = size <= 0 ? 10 : Math.min(size, 100);
		return PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	private PayrollPageDTO toPayrollPage(Page<PayrollEntity> page) {
		return new PayrollPageDTO(
				payrollMapper.toDtoList(page.getContent()),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext(),
				page.hasPrevious()
		);
	}

	private LocalDateTime toEndOfDay(LocalDate date) {
		return date.plusDays(1).atStartOfDay().minusNanos(1);
	}

	private VariableKey toVariableKey(String role) {
		String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "BURUH" -> VariableKey.UPAH_BURUH;
			case "SUPIR" -> VariableKey.UPAH_SUPIR;
			case "MANDOR" -> VariableKey.UPAH_MANDOR;
			default -> throw new IllegalArgumentException("Unsupported payroll role: " + role);
		};
	}
}
