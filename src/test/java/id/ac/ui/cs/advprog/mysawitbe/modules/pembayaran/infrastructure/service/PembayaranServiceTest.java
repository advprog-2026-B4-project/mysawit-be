package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpResponseDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PaymentGatewayPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PayrollRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.WalletRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

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
                grossAmount,
                "settlement",
                "credit_card",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);

        service.handlePaymentCallback(callback);

        verify(walletRepository).creditTopUp(eq(adminId), eq(grossAmount), eq(orderId));
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
                grossAmount,
                "capture",
                "gopay",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);

        service.handlePaymentCallback(callback);

        verify(walletRepository).creditTopUp(eq(adminId), eq(grossAmount), eq(orderId));
    }

    @Test
    void handlePaymentCallback_doesNotCreditOnDeny() {
        UUID adminId = UUID.randomUUID();
        String orderId = "TOPUP:" + adminId + ":" + UUID.randomUUID();
        String signature = "fake-signature";

        PaymentCallbackDTO callback = new PaymentCallbackDTO(
                "tx-deny",
                orderId,
                50000,
                "deny",
                "credit_card",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);

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
                50000,
                "expire",
                "bank_transfer",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);

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
                50000,
                "settlement",
                "credit_card",
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
                50000,
                "settlement",
                "credit_card",
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
                50000,
                "settlement",
                "credit_card",
                signature
        );

        when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
        when(paymentGateway.verifyCallbackSignature(callback)).thenReturn(true);

        service.handlePaymentCallback(callback);

        verify(walletRepository, never()).creditTopUp(any(), anyInt(), any());
    }
}