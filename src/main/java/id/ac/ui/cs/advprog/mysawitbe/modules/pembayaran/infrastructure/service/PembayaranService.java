package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.event.PayrollProcessedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.WalletQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PaymentGatewayPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PayrollRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.WalletRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PembayaranService implements PembayaranQueryUseCase, PembayaranCommandUseCase, WalletQueryUseCase {

	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String STATUS_REJECTED = "REJECTED";

	private static final String ROLE_BURUH = "BURUH";
	private static final String ROLE_MANDOR = "MANDOR";

	private static final String REFERENCE_PANEN = "PANEN";
	private static final String REFERENCE_PENGIRIMAN = "PENGIRIMAN";

	private static final Set<String> ALLOWED_STATUSES = Set.of(
			STATUS_PENDING,
			STATUS_APPROVED,
			STATUS_REJECTED
	);

	private final PayrollRepositoryPort payrollRepository;
	private final WalletRepositoryPort walletRepository;
	private final ObjectProvider<PaymentGatewayPort> paymentGatewayProvider;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public PayrollStatusDTO getPayrollStatus(UUID payrollId) {
		PayrollStatusDTO status = payrollRepository.findStatusById(payrollId);
		if (status == null) {
			throw new EntityNotFoundException("Payroll not found: " + payrollId);
		}
		return status;
	}

	@Override
	public PayrollPageDTO getPayrollsByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status, int page, int size) {
		validateDateRange(startDate, endDate);
		return payrollRepository.findByUserId(userId, startDate, endDate, normalizeStatus(status), page, size);
	}

	@Override
	public PayrollPageDTO listAllPayrolls(LocalDate startDate, LocalDate endDate, String status, int page, int size) {
		validateDateRange(startDate, endDate);
		return payrollRepository.findAll(startDate, endDate, normalizeStatus(status), page, size);
	}

	@Override
	@EventListener
	@Transactional
	public void onPanenApproved(PanenApprovedEvent event) {
		if (event == null || event.weight() <= 0) {
			return;
		}
		createPendingPayroll(
				event.buruhId(),
				ROLE_BURUH,
				event.panenId(),
				REFERENCE_PANEN,
				event.weight()
		);
	}

	@Override
	@EventListener
	@Transactional
	public void onPengirimanProcessedByAdmin(PengirimanProcessedByAdminEvent event) {
		if (event == null || event.acceptedWeight() <= 0) {
			return;
		}

		String normalizedStatus = event.status() == null ? "" : event.status().trim().toUpperCase(Locale.ROOT);
		if (STATUS_REJECTED.equals(normalizedStatus)) {
			return;
		}

		createPendingPayroll(
				event.mandorId(),
				ROLE_MANDOR,
				event.pengirimanId(),
				REFERENCE_PENGIRIMAN,
				event.acceptedWeight()
		);
	}

	@Override
	@Transactional
	public PayrollDTO approvePayroll(UUID payrollId, UUID adminId) {
		requireAdminId(adminId);
		PayrollDTO current = requirePayroll(payrollId);
		ensurePending(current);

		PayrollDTO approved = new PayrollDTO(
				current.payrollId(),
				current.userId(),
				current.role(),
				current.referenceId(),
				current.referenceType(),
				current.weight(),
				current.wageRateApplied(),
				current.netAmount(),
				STATUS_APPROVED,
				null,
				LocalDateTime.now(),
				current.createdAt()
		);

		PayrollDTO saved = payrollRepository.save(approved);
		walletRepository.credit(saved.userId(), saved.netAmount(), saved.payrollId());
		eventPublisher.publishEvent(new PayrollProcessedEvent(saved.payrollId(), saved.userId(), STATUS_APPROVED));
		return saved;
	}

	@Override
	@Transactional
	public PayrollDTO rejectPayroll(UUID payrollId, UUID adminId, String reason) {
		requireAdminId(adminId);
		if (reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("Reject reason is required");
		}

		PayrollDTO current = requirePayroll(payrollId);
		ensurePending(current);

		PayrollDTO rejected = new PayrollDTO(
				current.payrollId(),
				current.userId(),
				current.role(),
				current.referenceId(),
				current.referenceType(),
				current.weight(),
				current.wageRateApplied(),
				current.netAmount(),
				STATUS_REJECTED,
				reason.trim(),
				LocalDateTime.now(),
				current.createdAt()
		);

		PayrollDTO saved = payrollRepository.save(rejected);
		eventPublisher.publishEvent(new PayrollProcessedEvent(saved.payrollId(), saved.userId(), STATUS_REJECTED));
		return saved;
	}

	@Override
	@Transactional
	public void handlePaymentCallback(PaymentCallbackDTO payload) {
		PaymentGatewayPort paymentGateway = paymentGatewayProvider.getIfAvailable();
		if (paymentGateway == null) {
			throw new IllegalStateException("Payment gateway integration is not configured");
		}

		if (!paymentGateway.verifyCallbackSignature(payload)) {
			throw new IllegalArgumentException("Invalid payment callback signature");
		}
	}

	@Override
	@Transactional
	public void updateWageRate(String type, int newRatePerGram) {
		if (newRatePerGram <= 0) {
			throw new IllegalArgumentException("Wage rate must be positive");
		}
		payrollRepository.updateWageRate(type, newRatePerGram);
	}

	@Override
	public WalletBalanceDTO getUserWalletBalance(UUID userId) {
		return walletRepository.findBalanceByUserId(userId);
	}

	@Override
	public List<WalletTransactionDTO> getWalletTransactions(UUID userId) {
		return walletRepository.findTransactionsByUserId(userId);
	}

	private PayrollDTO createPendingPayroll(UUID userId, String role, UUID referenceId, String referenceType, int weight) {
		int wageRate = payrollRepository.getWageRate(role);
		int netAmount = multiplySafe(weight, wageRate);

		PayrollDTO pending = new PayrollDTO(
				null,
				userId,
				role,
				referenceId,
				referenceType,
				weight,
				wageRate,
				netAmount,
				STATUS_PENDING,
				null,
				null,
				LocalDateTime.now()
		);
		return payrollRepository.save(pending);
	}

	private PayrollDTO requirePayroll(UUID payrollId) {
		PayrollDTO payroll = payrollRepository.findById(payrollId);
		if (payroll == null) {
			throw new EntityNotFoundException("Payroll not found: " + payrollId);
		}
		return payroll;
	}

	private void ensurePending(PayrollDTO payroll) {
		if (!STATUS_PENDING.equalsIgnoreCase(payroll.status())) {
			throw new IllegalStateException("Only pending payroll can be processed");
		}
	}

	private void requireAdminId(UUID adminId) {
		if (adminId == null) {
			throw new IllegalArgumentException("Admin id is required");
		}
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("End date cannot be before start date");
		}
	}

	private String normalizeStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}

		String normalized = status.trim().toUpperCase(Locale.ROOT);
		if (!ALLOWED_STATUSES.contains(normalized)) {
			throw new IllegalArgumentException("Invalid payroll status: " + status);
		}
		return normalized;
	}

	private int multiplySafe(int left, int right) {
		long result = (long) left * right;
		if (result > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Payroll amount exceeds maximum supported value");
		}
		return (int) result;
	}
}
