package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
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
import static org.mockito.ArgumentMatchers.any;
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
}
