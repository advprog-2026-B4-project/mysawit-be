package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO.PhotoDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpResponseDTO;
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
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import id.ac.ui.cs.advprog.mysawitbe.common.port.DomainEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PembayaranService implements PembayaranQueryUseCase, PembayaranCommandUseCase, WalletQueryUseCase {

	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String STATUS_REJECTED = "REJECTED";

	private static final String ROLE_BURUH = "BURUH";
	private static final String ROLE_SUPIR = "SUPIR";
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
	private final PanenQueryUseCase panenQueryUseCase;
	private final UserQueryUseCase userQueryUseCase;
	private final ObjectProvider<PaymentGatewayPort> paymentGatewayProvider;
	private final DomainEventPublisher eventPublisher;
	private final MeterRegistry meterRegistry;

	private Counter payrollApprovedCounter;
	private Counter payrollRejectedCounter;

	@PostConstruct
	void initMetrics() {
		Gauge.builder("payroll.pending.count", payrollRepository, PayrollRepositoryPort::countPendingPayrolls)
				.description("Jumlah payroll berstatus PENDING — alert bila menumpuk")
				.register(meterRegistry);

		Gauge.builder("wallet.balance.total.rupiah", walletRepository, WalletRepositoryPort::sumAllWorkerBalances)
				.description("Total saldo seluruh wallet pekerja (Rupiah) — monitoring kewajiban finansial")
				.baseUnit("rupiah")
				.register(meterRegistry);

		payrollApprovedCounter = Counter.builder("payroll.approved.total")
				.description("Jumlah payroll disetujui admin (cumulative)")
				.register(meterRegistry);

		payrollRejectedCounter = Counter.builder("payroll.rejected.total")
				.description("Jumlah payroll ditolak admin (cumulative)")
				.register(meterRegistry);
	}

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
		PayrollPageDTO payrollPage = payrollRepository.findByUserId(userId, startDate, endDate, normalizeStatus(status), page, size);
		return enrichPayrollPageWithPanenEvidence(payrollPage);
	}

	@Override
	public PayrollPageDTO listAllPayrolls(LocalDate startDate, LocalDate endDate, String status, int page, int size) {
		validateDateRange(startDate, endDate);
		PayrollPageDTO payrollPage = payrollRepository.findAll(startDate, endDate, normalizeStatus(status), page, size);
		return enrichPayrollPageWithPanenEvidence(payrollPage);
	}

	private PayrollPageDTO enrichPayrollPageWithPanenEvidence(PayrollPageDTO page) {
		// Collect all panen IDs from this page in one batch — eliminates N+1
		Set<UUID> panenIds = page.items().stream()
				.filter(p -> REFERENCE_PANEN.equalsIgnoreCase(p.referenceType()))
				.map(PayrollDTO::referenceId)
                .filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Map<UUID, PanenDTO> panenById = panenIds.isEmpty()
				? Map.of()
				: panenQueryUseCase.getPanenByIds(panenIds);

		List<PayrollDTO> enrichedItems = page.items().stream()
				.map(payroll -> {
					if (!REFERENCE_PANEN.equalsIgnoreCase(payroll.referenceType())) {
						return copyPayrollWithEvidenceUrls(payroll, List.of());
					}
					var panen = panenById.get(payroll.referenceId());
					if (panen == null || panen.photos() == null) {
						return copyPayrollWithEvidenceUrls(payroll, List.of());
					}
					List<String> urls = panen.photos().stream()
							.map(PhotoDTO::url)
							.filter(url -> url != null && !url.isBlank())
							.distinct()
							.toList();
					return copyPayrollWithEvidenceUrls(payroll, urls);
				})
				.toList();

		return new PayrollPageDTO(
				enrichedItems,
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages(),
				page.hasNext(),
				page.hasPrevious()
		);
	}

	private PayrollDTO copyPayrollWithEvidenceUrls(PayrollDTO payroll, List<String> evidenceUrls) {
		return new PayrollDTO(
				payroll.payrollId(),
				payroll.userId(),
				payroll.role(),
				payroll.referenceId(),
				payroll.referenceType(),
				payroll.weight(),
				payroll.wageRateApplied(),
				payroll.netAmount(),
				payroll.status(),
				payroll.rejectionReason(),
				payroll.processedAt(),
				payroll.createdAt(),
				evidenceUrls
		);
	}

	@Override
	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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
		if (event.mandorId() != null) {
			PayrollDTO mandorPayroll = createPendingPayroll(
					event.mandorId(),
					ROLE_MANDOR,
					event.panenId(),
					REFERENCE_PANEN,
					event.weight()
			);
			if (mandorPayroll != null) {
				UUID adminId = userQueryUseCase.getAnyAdminId();
				autoApprovePayroll(mandorPayroll, adminId);
			}
		}
	}

	@Override
	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPengirimanApprovedByMandor(PengirimanApprovedByMandorEvent event) {
		if (event == null || event.totalWeight() <= 0) {
			return;
		}
		createPendingPayroll(
				event.supirId(),
				ROLE_SUPIR,
				event.pengirimanId(),
				REFERENCE_PENGIRIMAN,
				event.totalWeight()
		);
	}

	@Override
	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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
	@CacheEvict(value = {"wallet-balance", "wallet-tx"}, allEntries = true)
	public PayrollDTO approvePayroll(UUID payrollId, UUID adminId) {
		requireAdminId(adminId);
		PayrollDTO current = requirePayroll(payrollId);
		ensurePending(current);
		walletRepository.debit(adminId, current.netAmount(), current.payrollId());

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
		eventPublisher.publish(new PayrollProcessedEvent(saved.payrollId(), saved.userId(), STATUS_APPROVED));
		payrollApprovedCounter.increment();
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
		eventPublisher.publish(new PayrollProcessedEvent(saved.payrollId(), saved.userId(), STATUS_REJECTED));
		payrollRejectedCounter.increment();
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

		// Double verification: Fetch directly from Midtrans API to prevent spoofing
		PaymentCallbackDTO verifiedPayload = paymentGateway.fetchTransactionStatus(payload.orderId());
		if (verifiedPayload == null) {
			throw new IllegalStateException("Failed to fetch transaction status from Midtrans");
		}

		String status = verifiedPayload.transactionStatus() == null ? "" : verifiedPayload.transactionStatus().trim().toLowerCase(Locale.ROOT);
		if ("settlement".equals(status) || "capture".equals(status)) {
			String orderId = verifiedPayload.orderId();
			if (orderId != null && orderId.startsWith("TOPUP:")) {
				String[] parts = orderId.split(":", 3);
				if (parts.length == 3) {
					UUID adminId = UUID.fromString(parts[1]);
					long amountRupiah = new java.math.BigDecimal(verifiedPayload.grossAmount())
							.setScale(0, java.math.RoundingMode.UNNECESSARY)
							.longValueExact();
					walletRepository.creditTopUp(adminId, amountRupiah, verifiedPayload.orderId());
				}
			}
		}
	}

	@Override
	@Transactional
	public TopUpResponseDTO initiateTopUp(UUID adminId, int amount) {
		requireAdminId(adminId);
		if (amount <= 0) {
			throw new IllegalArgumentException("Amount must be positive");
		}
		if (amount % 10000 != 0) {
			throw new IllegalArgumentException("Amount must be a multiple of 10000 (Rp10.000 = $1)");
		}

		String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID().toString().substring(0, 5);
		PaymentGatewayPort paymentGateway = paymentGatewayProvider.getIfAvailable();
		if (paymentGateway == null) {
			throw new IllegalStateException("Payment gateway integration is not configured");
		}

		String redirectUrl = paymentGateway.initiateTopUp(orderId, amount);
		return new TopUpResponseDTO(redirectUrl);
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
	@Cacheable(value = "wallet-balance", key = "#userId")
	public WalletBalanceDTO getUserWalletBalance(UUID userId) {
		return walletRepository.findBalanceByUserId(userId);
	}

	@Override
	@Cacheable(value = "wallet-tx", key = "#userId")
	public List<WalletTransactionDTO> getWalletTransactions(UUID userId) {
		return walletRepository.findTransactionsByUserId(userId);
	}

	private PayrollDTO createPendingPayroll(UUID userId, String role, UUID referenceId, String referenceType, int weight) {
		if (payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
				userId,
				role,
				referenceId,
				referenceType
		)) {
			return null;
		}

		int wageRate = payrollRepository.getWageRate(role);
		int netAmount = computeWageFromGrams(weight, wageRate);

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

	private void autoApprovePayroll(PayrollDTO payroll, UUID adminId) {
		walletRepository.debit(adminId, payroll.netAmount(), payroll.payrollId());
		PayrollDTO approved = new PayrollDTO(
				payroll.payrollId(),
				payroll.userId(),
				payroll.role(),
				payroll.referenceId(),
				payroll.referenceType(),
				payroll.weight(),
				payroll.wageRateApplied(),
				payroll.netAmount(),
				STATUS_APPROVED,
				null,
				LocalDateTime.now(),
				payroll.createdAt()
		);
		PayrollDTO saved = payrollRepository.save(approved);
		walletRepository.credit(saved.userId(), saved.netAmount(), saved.payrollId());
		eventPublisher.publish(new PayrollProcessedEvent(saved.payrollId(), saved.userId(), STATUS_APPROVED));
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

	private int computeWageFromGrams(int weight, int wageRate) {
		long result = (long) weight * wageRate;
		if (result > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Payroll amount exceeds maximum supported value");
		}
		return (int) result;
	}
}
