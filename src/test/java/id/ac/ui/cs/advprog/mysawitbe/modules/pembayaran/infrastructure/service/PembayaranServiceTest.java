package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpResponseDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PaymentGatewayPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PayrollRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.WalletRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PembayaranServiceTest {

    @Mock
    private PayrollRepositoryPort payrollRepository;

    @Mock
    private WalletRepositoryPort walletRepository;

    @Mock
    private PanenQueryUseCase panenQueryUseCase;

    @Mock
    private ObjectProvider<PaymentGatewayPort> paymentGatewayProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MidtransProperties midtransProperties;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @InjectMocks
    private PembayaranService service;

    private UUID pengirimanId;
    private UUID supirId;
    private UUID mandorId;

    @BeforeEach
    void setUp() {
        pengirimanId = UUID.randomUUID();
        supirId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
    }

    @Test
    void onPengirimanApprovedByMandor_createsPendingPayrollForSupir() {
        PengirimanApprovedByMandorEvent event = new PengirimanApprovedByMandorEvent(
                pengirimanId,
                supirId,
                320000
        );

        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                supirId,
                "SUPIR",
                pengirimanId,
                "PENGIRIMAN"
        )).thenReturn(false);
        when(payrollRepository.getWageRate("SUPIR")).thenReturn(8);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPengirimanApprovedByMandor(event);

        ArgumentCaptor<PayrollDTO> payrollCaptor = ArgumentCaptor.forClass(PayrollDTO.class);
        verify(payrollRepository).save(payrollCaptor.capture());

        PayrollDTO saved = payrollCaptor.getValue();
        assertThat(saved.userId()).isEqualTo(supirId);
        assertThat(saved.role()).isEqualTo("SUPIR");
        assertThat(saved.referenceId()).isEqualTo(pengirimanId);
        assertThat(saved.referenceType()).isEqualTo("PENGIRIMAN");
        assertThat(saved.weight()).isEqualTo(320000);
        assertThat(saved.wageRateApplied()).isEqualTo(8);
        assertThat(saved.netAmount()).isEqualTo(2560000);
        assertThat(saved.status()).isEqualTo("PENDING");
    }

    @Test
    void onPengirimanApprovedByMandor_duplicateEvent_doesNotCreateDuplicatePayroll() {
        PengirimanApprovedByMandorEvent event = new PengirimanApprovedByMandorEvent(
                pengirimanId,
                supirId,
                320000
        );

        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                supirId,
                "SUPIR",
                pengirimanId,
                "PENGIRIMAN"
        )).thenReturn(true);

        service.onPengirimanApprovedByMandor(event);

        verify(payrollRepository, never()).getWageRate(any());
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void onPengirimanApprovedByMandor_nullOrNonPositiveWeight_isIgnored() {
        service.onPengirimanApprovedByMandor(null);
        service.onPengirimanApprovedByMandor(new PengirimanApprovedByMandorEvent(
                pengirimanId,
                supirId,
                0
        ));

        verifyNoInteractions(payrollRepository);
    }

    @Test
    void onPengirimanProcessedByAdmin_keepsCreatingPendingPayrollForMandor() {
        PengirimanProcessedByAdminEvent event = new PengirimanProcessedByAdminEvent(
                pengirimanId,
                mandorId,
                210000,
                "APPROVED"
        );

        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                mandorId,
                "MANDOR",
                pengirimanId,
                "PENGIRIMAN"
        )).thenReturn(false);
        when(payrollRepository.getWageRate("MANDOR")).thenReturn(12);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPengirimanProcessedByAdmin(event);

        ArgumentCaptor<PayrollDTO> payrollCaptor = ArgumentCaptor.forClass(PayrollDTO.class);
        verify(payrollRepository).save(payrollCaptor.capture());

        PayrollDTO saved = payrollCaptor.getValue();
        assertThat(saved.userId()).isEqualTo(mandorId);
        assertThat(saved.role()).isEqualTo("MANDOR");
        assertThat(saved.referenceId()).isEqualTo(pengirimanId);
        assertThat(saved.referenceType()).isEqualTo("PENGIRIMAN");
        assertThat(saved.weight()).isEqualTo(210000);
    }

    // --- Top-up tests ---

    @Test
    void initiateTopUp_callsPaymentGateway_andReturnsPaymentUrl() {
        UUID adminId = UUID.randomUUID();
        int amount = 50000;
        String expectedRedirect = "https://app.midtrans.com/snap/v2/v1/transactions?token=fake-token";

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.initiateTopUp(anyString(), eq(amount))).thenReturn(expectedRedirect);

        TopUpResponseDTO result = service.initiateTopUp(adminId, amount);

        assertThat(result.paymentUrl()).isEqualTo(expectedRedirect);
        ArgumentCaptor<String> orderIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(paymentGateway).initiateTopUp(orderIdCaptor.capture(), eq(amount));
        assertThat(orderIdCaptor.getValue()).startsWith("TOPUP:" + adminId);
    }

    @Test
    void initiateTopUp_throwsWhenAmountIsNotPositive() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> service.initiateTopUp(adminId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");

        assertThatThrownBy(() -> service.initiateTopUp(adminId, -50000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    @Test
    void initiateTopUp_throwsWhenAdminIdIsNull() {
        assertThatThrownBy(() -> service.initiateTopUp(null, 50000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin id is required");
    }

    @Test
    void initiateTopUp_throwsWhenPaymentGatewayNotConfigured() {
        UUID adminId = UUID.randomUUID();
        when(paymentGatewayProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.initiateTopUp(adminId, 50000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment gateway integration is not configured");
    }

    @Test
    void handlePaymentCallback_creditsWalletOnSettlement() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID();
        int grossAmount = 100000;
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-123",
                orderId,
                String.valueOf(grossAmount),
                "settlement",
                "credit_card",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository).creditTopUp(eq(adminId), eq(grossAmount / 10000), eq(orderId));
    }

    @Test
    void handlePaymentCallback_creditsWalletOnCapture() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID();
        int grossAmount = 75000;
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-456",
                orderId,
                String.valueOf(grossAmount),
                "capture",
                "gopay",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository).creditTopUp(eq(adminId), eq(grossAmount / 10000), eq(orderId));
    }

    @Test
    void handlePaymentCallback_doesNotCreditOnDeny() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID();
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-deny",
                orderId,
                "50000",
                "deny",
                "credit_card",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_doesNotCreditOnExpire() {
        String orderId = "TOPUP:" + UUID.randomUUID() + ":" + UUID.randomUUID();
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-expire",
                orderId,
                "50000",
                "expire",
                "bank_transfer",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_throwsOnInvalidSignature() {
        String orderId = "TOPUP:" + UUID.randomUUID() + ":" + UUID.randomUUID();
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-invalid",
                orderId,
                "50000",
                "settlement",
                "credit_card",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(false);

        assertThatThrownBy(() -> service.handlePaymentCallback(callback))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid payment callback signature");

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_throwsWhenPaymentGatewayNotConfigured() {
        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-123",
                "TOPUP:" + UUID.randomUUID() + ":" + UUID.randomUUID(),
                "50000",
                "settlement",
                "credit_card",
                "200",
                "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.handlePaymentCallback(callback))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment gateway integration is not configured");
    }

    @Test
    void handlePaymentCallback_ignoresNonTopupOrderId() {
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-other",
                "SOME-OTHER-ORDER-ID",
                "50000",
                "settlement",
                "credit_card",
                "200",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    // --- getPayrollStatus ---

    @Test
    void getPayrollStatus_returnsStatusDTO() {
        UUID payrollId = UUID.randomUUID();
        PayrollStatusDTO expected = new PayrollStatusDTO(payrollId, UUID.randomUUID(), 50000, "PENDING", null, null);
        when(payrollRepository.findStatusById(payrollId)).thenReturn(expected);

        PayrollStatusDTO result = service.getPayrollStatus(payrollId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getPayrollStatus_throwsEntityNotFoundExceptionWhenNotFound() {
        UUID payrollId = UUID.randomUUID();
        when(payrollRepository.findStatusById(payrollId)).thenReturn(null);

        assertThatThrownBy(() -> service.getPayrollStatus(payrollId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payroll not found");
    }

    // --- getPayrollsByUserId ---

    @Test
    void getPayrollsByUserId_returnsPagedResults() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(eq(userId), eq(start), eq(end), eq("PENDING"), eq(0), eq(10))).thenReturn(repoResult);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, start, end, "PENDING", 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getPayrollsByUserId_nullStatusReturnsNullFilter() {
        UUID userId = UUID.randomUUID();
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);

        when(payrollRepository.findByUserId(eq(userId), any(), any(), eq(null), eq(0), eq(10))).thenReturn(repoResult);

        service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        verify(payrollRepository).findByUserId(userId, null, null, null, 0, 10);
    }

    @Test
    void getPayrollsByUserId_enrichesPanenWithEvidence() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        PanenDTO panen = new PanenDTO(panenId, userId, "Buruh Satu", UUID.randomUUID(), "desc", 1000, "APPROVED", null, List.of(new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo1.jpg"), new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo2.jpg")), LocalDateTime.now());
        when(panenQueryUseCase.getPanenById(panenId)).thenReturn(panen);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).containsExactly("http://photo1.jpg", "http://photo2.jpg");
    }

    @Test
    void getPayrollsByUserId_nonPanenReferenceGetsEmptyEvidence() {
        UUID userId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "SUPIR", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).isEmpty();
    }

    @Test
    void getPayrollsByUserId_panenNotFoundGivesEmptyEvidence() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        when(panenQueryUseCase.getPanenById(panenId)).thenThrow(new RuntimeException("not found"));

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).isEmpty();
    }

    // --- listAllPayrolls ---

    @Test
    void listAllPayrolls_returnsPagedResults() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), UUID.randomUUID(), "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "APPROVED", null, LocalDateTime.now(), LocalDateTime.now());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findAll(eq(start), eq(end), eq("APPROVED"), eq(0), eq(10))).thenReturn(repoResult);

        PayrollPageDTO result = service.listAllPayrolls(start, end, "APPROVED", 0, 10);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listAllPayrolls_nullStatusReturnsNullFilter() {
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(payrollRepository.findAll(any(), any(), eq(null), eq(0), eq(10))).thenReturn(repoResult);

        service.listAllPayrolls(null, null, null, 0, 10);

        verify(payrollRepository).findAll(null, null, null, 0, 10);
    }

    // --- validateDateRange ---

    @Test
    void getPayrollsByUserId_throwsWhenStartDateAfterEndDate() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2024, 12, 31);
        LocalDate end = LocalDate.of(2024, 1, 1);

        assertThatThrownBy(() -> service.getPayrollsByUserId(userId, start, end, null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");
    }

    @Test
    void listAllPayrolls_throwsWhenStartDateAfterEndDate() {
        LocalDate start = LocalDate.of(2024, 12, 31);
        LocalDate end = LocalDate.of(2024, 1, 1);

        assertThatThrownBy(() -> service.listAllPayrolls(start, end, null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");
    }

    @Test
    void getPayrollsByUserId_acceptsNullDates() {
        UUID userId = UUID.randomUUID();
        when(payrollRepository.findByUserId(eq(userId), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false));

        service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        verify(payrollRepository).findByUserId(userId, null, null, null, 0, 10);
    }

    // --- normalizeStatus ---

    @Test
    void getPayrollsByUserId_throwsOnInvalidStatus() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getPayrollsByUserId(userId, null, null, "INVALID_STATUS", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payroll status");
    }

    @Test
    void listAllPayrolls_throwsOnInvalidStatus() {
        assertThatThrownBy(() -> service.listAllPayrolls(null, null, "INVALID_STATUS", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payroll status");
    }

    @Test
    void normalizeStatus_returnsNullForNullInput() {
        when(payrollRepository.findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt()))
                .thenReturn(new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false));

        service.getPayrollsByUserId(UUID.randomUUID(), null, null, null, 0, 10);

        verify(payrollRepository).findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt());
    }

    @Test
    void normalizeStatus_returnsNullForBlankInput() {
        when(payrollRepository.findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt()))
                .thenReturn(new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false));

        service.getPayrollsByUserId(UUID.randomUUID(), null, null, "   ", 0, 10);

        verify(payrollRepository).findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt());
    }

    // --- approvePayroll ---

    @Test
    void approvePayroll_success() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.now().minusHours(1);
        PayrollDTO pending = new PayrollDTO(payrollId, userId, "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "PENDING", null, null, created);

        when(payrollRepository.findById(payrollId)).thenReturn(pending);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollDTO result = service.approvePayroll(payrollId, adminId);

        assertThat(result.status()).isEqualTo("APPROVED");
        verify(walletRepository).debit(eq(adminId), eq(8000), eq(payrollId));
        verify(walletRepository).credit(eq(userId), eq(8000), eq(payrollId));
    }

    @Test
    void approvePayroll_throwsWhenPayrollNotFound() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(payrollRepository.findById(payrollId)).thenReturn(null);

        assertThatThrownBy(() -> service.approvePayroll(payrollId, adminId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payroll not found");
    }

    @Test
    void approvePayroll_throwsWhenNotPendingStatus() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PayrollDTO approved = new PayrollDTO(payrollId, UUID.randomUUID(), "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "APPROVED", null, null, LocalDateTime.now());

        when(payrollRepository.findById(payrollId)).thenReturn(approved);

        assertThatThrownBy(() -> service.approvePayroll(payrollId, adminId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only pending payroll can be processed");
    }

    @Test
    void approvePayroll_throwsWhenAdminIdNull() {
        UUID payrollId = UUID.randomUUID();

        assertThatThrownBy(() -> service.approvePayroll(payrollId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin id is required");
    }

    // --- rejectPayroll ---

    @Test
    void rejectPayroll_success() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.now().minusHours(1);
        PayrollDTO pending = new PayrollDTO(payrollId, userId, "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "PENDING", null, null, created);

        when(payrollRepository.findById(payrollId)).thenReturn(pending);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollDTO result = service.rejectPayroll(payrollId, adminId, "Not valid");

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectionReason()).isEqualTo("Not valid");
        verify(walletRepository, never()).debit(any(), anyInt(), any());
        verify(walletRepository, never()).credit(any(), anyInt(), any());
    }

    @Test
    void rejectPayroll_throwsWhenPayrollNotFound() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(payrollRepository.findById(payrollId)).thenReturn(null);

        assertThatThrownBy(() -> service.rejectPayroll(payrollId, adminId, "reason"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payroll not found");
    }

    @Test
    void rejectPayroll_throwsWhenNotPendingStatus() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PayrollDTO rejected = new PayrollDTO(payrollId, UUID.randomUUID(), "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "REJECTED", "Already rejected", null, LocalDateTime.now());

        when(payrollRepository.findById(payrollId)).thenReturn(rejected);

        assertThatThrownBy(() -> service.rejectPayroll(payrollId, adminId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only pending payroll can be processed");
    }

    @Test
    void rejectPayroll_throwsWhenReasonNull() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> service.rejectPayroll(payrollId, adminId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reject reason is required");
    }

    @Test
    void rejectPayroll_throwsWhenReasonBlank() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> service.rejectPayroll(payrollId, adminId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reject reason is required");
    }

    @Test
    void rejectPayroll_throwsWhenAdminIdNull() {
        UUID payrollId = UUID.randomUUID();

        assertThatThrownBy(() -> service.rejectPayroll(payrollId, null, "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin id is required");
    }

    // --- updateWageRate ---

    @Test
    void updateWageRate_success() {
        service.updateWageRate("BURUH", 10);

        verify(payrollRepository).updateWageRate("BURUH", 10);
    }

    @Test
    void updateWageRate_throwsWhenRateIsZero() {
        assertThatThrownBy(() -> service.updateWageRate("BURUH", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wage rate must be positive");
    }

    @Test
    void updateWageRate_throwsWhenRateIsNegative() {
        assertThatThrownBy(() -> service.updateWageRate("BURUH", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wage rate must be positive");
    }

    // --- getUserWalletBalance ---

    @Test
    void getUserWalletBalance_delegatesToRepository() {
        UUID userId = UUID.randomUUID();
        WalletBalanceDTO expected = new WalletBalanceDTO(userId, 100000, LocalDateTime.now());
        when(walletRepository.findBalanceByUserId(userId)).thenReturn(expected);

        WalletBalanceDTO result = service.getUserWalletBalance(userId);

        assertThat(result).isEqualTo(expected);
    }

    // --- getWalletTransactions ---

    @Test
    void getWalletTransactions_delegatesToRepository() {
        UUID userId = UUID.randomUUID();
        List<WalletTransactionDTO> expected = List.of(new WalletTransactionDTO(UUID.randomUUID(), userId, UUID.randomUUID(), 50000, "CREDIT", "ref", LocalDateTime.now()));
        when(walletRepository.findTransactionsByUserId(userId)).thenReturn(expected);

        List<WalletTransactionDTO> result = service.getWalletTransactions(userId);

        assertThat(result).isEqualTo(expected);
    }

    // --- multiplySafe ---

    @Test
    void createPendingPayroll_usesMultiplySafeNormalCase() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(userId, "BURUH", referenceId, "PANEN")).thenReturn(false);
        when(payrollRepository.getWageRate("BURUH")).thenReturn(8);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                referenceId, userId, UUID.randomUUID(), 100000, LocalDateTime.now()
        ));

        ArgumentCaptor<PayrollDTO> captor = ArgumentCaptor.forClass(PayrollDTO.class);
        verify(payrollRepository).save(captor.capture());
        assertThat(captor.getValue().netAmount()).isEqualTo(800000);
    }

    @Test
    void createPendingPayroll_throwsOnMultiplyOverflow() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(userId, "BURUH", referenceId, "PANEN")).thenReturn(false);
        when(payrollRepository.getWageRate("BURUH")).thenReturn(Integer.MAX_VALUE);

        assertThatThrownBy(() -> service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                referenceId, userId, UUID.randomUUID(), 100000, LocalDateTime.now()
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payroll amount exceeds maximum supported value");
    }

    // --- onPanenApproved ---

    @Test
    void onPanenApproved_createsPendingPayrollForBuruh() {
        UUID buruhId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(buruhId, "BURUH", panenId, "PANEN")).thenReturn(false);
        when(payrollRepository.getWageRate("BURUH")).thenReturn(8);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                panenId, buruhId, mandorId, 50000, LocalDateTime.now()
        ));

        ArgumentCaptor<PayrollDTO> captor = ArgumentCaptor.forClass(PayrollDTO.class);
        verify(payrollRepository).save(captor.capture());
        PayrollDTO saved = captor.getValue();
        assertThat(saved.userId()).isEqualTo(buruhId);
        assertThat(saved.role()).isEqualTo("BURUH");
        assertThat(saved.referenceId()).isEqualTo(panenId);
        assertThat(saved.referenceType()).isEqualTo("PANEN");
        assertThat(saved.weight()).isEqualTo(50000);
        assertThat(saved.netAmount()).isEqualTo(400000);
        assertThat(saved.status()).isEqualTo("PENDING");
    }

    @Test
    void onPanenApproved_ignoresNullEvent() {
        service.onPanenApproved(null);
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void onPanenApproved_ignoresNonPositiveWeight() {
        service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, LocalDateTime.now()
        ));
        service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), -100, LocalDateTime.now()
        ));
        verifyNoInteractions(payrollRepository);
    }

    // --- enrichPayrollWithPanenEvidence ---

    @Test
    void getPayrollsByUserId_enrichesPanenWithNullPhotosGivesEmptyEvidence() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now(), List.of());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        PanenDTO panenWithNullPhotos = new PanenDTO(panenId, userId, "Buruh Satu", UUID.randomUUID(), "desc", 1000, "APPROVED", null, null, LocalDateTime.now());
        when(panenQueryUseCase.getPanenById(panenId)).thenReturn(panenWithNullPhotos);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).isEmpty();
    }

    @Test
    void getPayrollsByUserId_enrichesPanenWithSomeNullPhotoUrls() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now(), List.of());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        PanenDTO panenWithNullUrls = new PanenDTO(
                panenId, userId, "Buruh Satu", UUID.randomUUID(), "desc", 1000, "APPROVED", null,
                List.of(new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo1.jpg"), new PanenDTO.PhotoDTO(UUID.randomUUID(), null), new PanenDTO.PhotoDTO(UUID.randomUUID(), "   ")),
                LocalDateTime.now()
        );
        when(panenQueryUseCase.getPanenById(panenId)).thenReturn(panenWithNullUrls);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).containsExactly("http://photo1.jpg");
    }

    @Test
    void getPayrollsByUserId_enrichesPanenWithDuplicateUrlsFiltered() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now(), List.of());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        PanenDTO panenWithDuplicates = new PanenDTO(
                panenId, userId, "Buruh Satu", UUID.randomUUID(), "desc", 1000, "APPROVED", null,
                List.of(
                        new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo1.jpg"),
                        new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo1.jpg"),
                        new PanenDTO.PhotoDTO(UUID.randomUUID(), "http://photo2.jpg")
                ),
                LocalDateTime.now()
        );
        when(panenQueryUseCase.getPanenById(panenId)).thenReturn(panenWithDuplicates);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).containsExactlyInAnyOrder("http://photo1.jpg", "http://photo2.jpg");
    }

    // --- onPengirimanProcessedByAdmin ---

    @Test
    void onPengirimanProcessedByAdmin_ignoresNullEvent() {
        service.onPengirimanProcessedByAdmin(null);
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void onPengirimanProcessedByAdmin_ignoresNonPositiveWeight() {
        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                UUID.randomUUID(), UUID.randomUUID(), 0, "APPROVED"
        ));
        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                UUID.randomUUID(), UUID.randomUUID(), -100, "APPROVED"
        ));
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void onPengirimanProcessedByAdmin_ignoresRejectedStatus() {
        UUID pengirimanId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        // When status is REJECTED, the method returns early before any repository call
        // No stubbing needed since the early return prevents any repository interaction

        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                pengirimanId, mandorId, 50000, "REJECTED"
        ));

        verify(payrollRepository, never()).existsByUserIdAndRoleAndReferenceIdAndReferenceType(any(), any(), any(), any());
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void onPengirimanProcessedByAdmin_nullStatusCreatesPayroll() {
        UUID pengirimanId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                mandorId, "MANDOR", pengirimanId, "PENGIRIMAN"
        )).thenReturn(false);
        when(payrollRepository.getWageRate("MANDOR")).thenReturn(12);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                pengirimanId, mandorId, 50000, null
        ));

        verify(payrollRepository).save(any(PayrollDTO.class));
    }

    @Test
    void onPengirimanProcessedByAdmin_blankStatusCreatesPayroll() {
        UUID pengirimanId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                mandorId, "MANDOR", pengirimanId, "PENGIRIMAN"
        )).thenReturn(false);
        when(payrollRepository.getWageRate("MANDOR")).thenReturn(12);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                pengirimanId, mandorId, 50000, "   "
        ));

        verify(payrollRepository).save(any(PayrollDTO.class));
    }

    @Test
    void onPengirimanProcessedByAdmin_acceptsNonRejectedStatus() {
        UUID pengirimanId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                mandorId, "MANDOR", pengirimanId, "PENGIRIMAN"
        )).thenReturn(false);
        when(payrollRepository.getWageRate("MANDOR")).thenReturn(12);
        when(payrollRepository.save(any(PayrollDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onPengirimanProcessedByAdmin(new PengirimanProcessedByAdminEvent(
                pengirimanId, mandorId, 50000, "APPROVED"
        ));

        ArgumentCaptor<PayrollDTO> captor = ArgumentCaptor.forClass(PayrollDTO.class);
        verify(payrollRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("PENDING");
    }

    @Test
    void getPayrollsByUserId_panenGetPanenByIdReturnsNullGivesEmptyEvidence() {
        UUID userId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PayrollDTO payroll = new PayrollDTO(UUID.randomUUID(), userId, "BURUH", panenId, "PANEN", 1000, 8, 8000, "PENDING", null, null, LocalDateTime.now(), List.of());
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(payroll), 0, 10, 1, 1, false, false);

        when(payrollRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(repoResult);
        when(panenQueryUseCase.getPanenById(panenId)).thenReturn(null);

        PayrollPageDTO result = service.getPayrollsByUserId(userId, null, null, null, 0, 10);

        assertThat(result.items().get(0).evidencePhotoUrls()).isEmpty();
    }

    // --- handlePaymentCallback additional branches ---

    @Test
    void handlePaymentCallback_throwsWhenFetchTransactionStatusReturnsNull() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID().toString().substring(0, 5);
        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-123", orderId, "50000", "settlement", "credit_card", "200", "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(null);

        assertThatThrownBy(() -> service.handlePaymentCallback(callback))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch transaction status from Midtrans");
    }

    @Test
    void handlePaymentCallback_ignoresWhenTransactionStatusIsNull() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID().toString().substring(0, 5);
        PaymentCallbackDTO callbackWithNullStatus = new PaymentCallbackDTO(
                "tx-123", orderId, "50000", null, "credit_card", "200", "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callbackWithNullStatus)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callbackWithNullStatus.orderId())).thenReturn(callbackWithNullStatus);

        service.handlePaymentCallback(callbackWithNullStatus);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_ignoresWhenOrderIdIsNull() {
        PaymentCallbackDTO callbackWithNullOrderId = new PaymentCallbackDTO(
                "tx-123", null, "50000", "settlement", "credit_card", "200", "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callbackWithNullOrderId)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(null)).thenReturn(callbackWithNullOrderId);

        service.handlePaymentCallback(callbackWithNullOrderId);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_ignoresWhenOrderIdDoesNotStartWithTopup() {
        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-123", "OTHER-ORDER-ID", "50000", "settlement", "credit_card", "200", "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    @Test
    void handlePaymentCallback_ignoresWhenOrderIdHasFewerThanThreeParts() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId;
        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-123", orderId, "50000", "settlement", "credit_card", "200", "sig"
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);
        when(paymentGateway.fetchTransactionStatus(callback.orderId())).thenReturn(callback);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }

    // --- initiateTopUp amount not multiple of 10000 ---

    @Test
    void initiateTopUp_throwsWhenAmountIsNotMultipleOf10000() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> service.initiateTopUp(adminId, 5000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be a multiple of 10000 (Rp10.000 = $1)");

        assertThatThrownBy(() -> service.initiateTopUp(adminId, 15000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be a multiple of 10000 (Rp10.000 = $1)");
    }

    // --- validateDateRange additional branches ---

    @Test
    void getPayrollsByUserId_acceptsNullEndDate() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2024, 1, 1);
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(payrollRepository.findByUserId(eq(userId), eq(start), eq(null), any(), anyInt(), anyInt())).thenReturn(repoResult);

        service.getPayrollsByUserId(userId, start, null, null, 0, 10);

        verify(payrollRepository).findByUserId(userId, start, null, null, 0, 10);
    }

    @Test
    void getPayrollsByUserId_acceptsNullStartDate() {
        UUID userId = UUID.randomUUID();
        LocalDate end = LocalDate.of(2024, 12, 31);
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(payrollRepository.findByUserId(eq(userId), eq(null), eq(end), any(), anyInt(), anyInt())).thenReturn(repoResult);

        service.getPayrollsByUserId(userId, null, end, null, 0, 10);

        verify(payrollRepository).findByUserId(userId, null, end, null, 0, 10);
    }

    @Test
    void listAllPayrolls_acceptsNullEndDate() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(payrollRepository.findAll(eq(start), eq(null), any(), anyInt(), anyInt())).thenReturn(repoResult);

        service.listAllPayrolls(start, null, null, 0, 10);

        verify(payrollRepository).findAll(start, null, null, 0, 10);
    }

    @Test
    void listAllPayrolls_acceptsNullStartDate() {
        LocalDate end = LocalDate.of(2024, 12, 31);
        PayrollPageDTO repoResult = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(payrollRepository.findAll(eq(null), eq(end), any(), anyInt(), anyInt())).thenReturn(repoResult);

        service.listAllPayrolls(null, end, null, 0, 10);

        verify(payrollRepository).findAll(null, end, null, 0, 10);
    }

    // --- normalizeStatus edge cases ---

    @Test
    void normalizeStatus_throwsOnInvalidStatusInListAllPayrolls() {
        assertThatThrownBy(() -> service.listAllPayrolls(null, null, "INVALID", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payroll status");
    }

    @Test
    void normalizeStatus_returnsNullForWhitespaceOnlyStatus() {
        when(payrollRepository.findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt()))
                .thenReturn(new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false));

        service.getPayrollsByUserId(UUID.randomUUID(), null, null, "  ", 0, 10);

        verify(payrollRepository).findByUserId(any(), any(), any(), eq(null), anyInt(), anyInt());
    }

    // --- multiplySafe ---

    @Test
    void multiplySafe_throwsOnOverflow() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(userId, "BURUH", referenceId, "PANEN")).thenReturn(false);
        when(payrollRepository.getWageRate("BURUH")).thenReturn(Integer.MAX_VALUE);

        assertThatThrownBy(() -> service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                referenceId, userId, UUID.randomUUID(), 100000, LocalDateTime.now()
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payroll amount exceeds maximum supported value");
    }

    // --- createPendingPayroll duplicate ---

    @Test
    void createPendingPayroll_skipsDuplicate() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(payrollRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(userId, "BURUH", referenceId, "PANEN")).thenReturn(true);

        service.onPanenApproved(new id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent(
                referenceId, userId, UUID.randomUUID(), 50000, LocalDateTime.now()
        ));

        verify(payrollRepository, never()).getWageRate(any());
        verify(payrollRepository, never()).save(any());
    }

    // --- ensurePending ---

    @Test
    void approvePayroll_throwsWhenStatusIsApproved() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PayrollDTO approved = new PayrollDTO(payrollId, UUID.randomUUID(), "BURUH", UUID.randomUUID(), "PENGIRIMAN", 1000, 8, 8000, "APPROVED", null, null, LocalDateTime.now());

        when(payrollRepository.findById(payrollId)).thenReturn(approved);

        assertThatThrownBy(() -> service.approvePayroll(payrollId, adminId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only pending payroll can be processed");
    }

    // --- requireAdminId ---

    @Test
    void approvePayroll_throwsWhenAdminIdIsNull() {
        UUID payrollId = UUID.randomUUID();

        assertThatThrownBy(() -> service.approvePayroll(payrollId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin id is required");
    }
}