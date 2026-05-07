package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.WalletQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PembayaranControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PembayaranQueryUseCase pembayaranQueryUseCase;
    @Mock
    private PembayaranCommandUseCase pembayaranCommandUseCase;
    @Mock
    private WalletQueryUseCase walletQueryUseCase;

    @InjectMocks
    private PembayaranController pembayaranController;

    private static final UUID PAYROLL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID REFERENCE_ID = UUID.randomUUID();

    private PayrollDTO samplePayrollDTO() {
        return new PayrollDTO(
                PAYROLL_ID,
                USER_ID,
                "PANEN",
                REFERENCE_ID,
                "PANEN",
                5000,
                10,
                50000,
                "PENDING",
                null,
                null,
                LocalDateTime.now()
        );
    }

    private PayrollStatusDTO samplePayrollStatusDTO() {
        return new PayrollStatusDTO(
                PAYROLL_ID,
                USER_ID,
                50000,
                "PENDING",
                LocalDateTime.now(),
                "PAYREF123"
        );
    }

    private PayrollPageDTO samplePayrollPageDTO() {
        return new PayrollPageDTO(
                List.of(samplePayrollDTO()),
                0,
                10,
                1,
                1,
                false,
                false
        );
    }

    private WalletBalanceDTO sampleWalletBalanceDTO() {
        return new WalletBalanceDTO(USER_ID, 100000, LocalDateTime.now());
    }

    private WalletTransactionDTO sampleWalletTransactionDTO() {
        return new WalletTransactionDTO(
                UUID.randomUUID(),
                USER_ID,
                UUID.randomUUID(),
                50000,
                "CREDIT",
                "MIDTRANS-123",
                LocalDateTime.now()
        );
    }

    private TopUpResponseDTO sampleTopUpResponseDTO() {
        return new TopUpResponseDTO("https://midtrans.url/payment");
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(pembayaranController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPayrollStatus_existingPayroll_returns200() throws Exception {
        PayrollStatusDTO statusDTO = samplePayrollStatusDTO();
        when(pembayaranQueryUseCase.getPayrollStatus(PAYROLL_ID)).thenReturn(statusDTO);

        mockMvc.perform(get("/api/pembayaran/payroll/{payrollId}/status", PAYROLL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payrollId").value(PAYROLL_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getPayrollStatus_notFound_returns404() throws Exception {
        when(pembayaranQueryUseCase.getPayrollStatus(PAYROLL_ID))
                .thenThrow(new EntityNotFoundException("Payroll not found"));

        mockMvc.perform(get("/api/pembayaran/payroll/{payrollId}/status", PAYROLL_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayrollsByUserId_returns200WithList() throws Exception {
        PayrollPageDTO pageDTO = samplePayrollPageDTO();
        when(pembayaranQueryUseCase.getPayrollsByUserId(eq(USER_ID), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(pageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].payrollId").value(PAYROLL_ID.toString()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getPayrollsByUserId_withFilters_returns200() throws Exception {
        PayrollPageDTO pageDTO = samplePayrollPageDTO();
        when(pembayaranQueryUseCase.getPayrollsByUserId(USER_ID, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31), "PENDING", 0, 10)).thenReturn(pageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll/user/{userId}", USER_ID)
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void getPayrollsByUserId_emptyList_returns200WithEmptyItems() throws Exception {
        PayrollPageDTO emptyPageDTO = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(pembayaranQueryUseCase.getPayrollsByUserId(eq(USER_ID), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(emptyPageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void listAllPayrolls_returns200WithList() throws Exception {
        PayrollPageDTO pageDTO = samplePayrollPageDTO();
        when(pembayaranQueryUseCase.listAllPayrolls(any(), any(), any(), eq(0), eq(10)))
                .thenReturn(pageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listAllPayrolls_withFilters_returns200() throws Exception {
        PayrollPageDTO pageDTO = samplePayrollPageDTO();
        when(pembayaranQueryUseCase.listAllPayrolls(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31), "PENDING", 0, 5)).thenReturn(pageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void listAllPayrolls_emptyList_returns200WithEmptyItems() throws Exception {
        PayrollPageDTO emptyPageDTO = new PayrollPageDTO(List.of(), 0, 10, 0, 0, false, false);
        when(pembayaranQueryUseCase.listAllPayrolls(any(), any(), any(), eq(0), eq(10)))
                .thenReturn(emptyPageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void listAllPayrolls_paginationDefaults_correctly() throws Exception {
        PayrollPageDTO pageDTO = samplePayrollPageDTO();
        when(pembayaranQueryUseCase.listAllPayrolls(null, null, null, 0, 10))
                .thenReturn(pageDTO);

        mockMvc.perform(get("/api/pembayaran/payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void approvePayroll_validRequest_returns200() throws Exception {
        PayrollDTO approvedDTO = new PayrollDTO(
                PAYROLL_ID, USER_ID, "PANEN", REFERENCE_ID, "PANEN",
                5000, 10, 50000, "APPROVED", null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(pembayaranCommandUseCase.approvePayroll(PAYROLL_ID, ADMIN_ID)).thenReturn(approvedDTO);

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/approve", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payroll approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void approvePayroll_notFound_returns404() throws Exception {
        when(pembayaranCommandUseCase.approvePayroll(PAYROLL_ID, ADMIN_ID))
                .thenThrow(new EntityNotFoundException("Payroll not found"));

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/approve", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectPayroll_validRequest_returns200() throws Exception {
        PayrollDTO rejectedDTO = new PayrollDTO(
                PAYROLL_ID, USER_ID, "PANEN", REFERENCE_ID, "PANEN",
                5000, 10, 50000, "REJECTED", "Quality not met", LocalDateTime.now(), LocalDateTime.now()
        );
        when(pembayaranCommandUseCase.rejectPayroll(eq(PAYROLL_ID), eq(ADMIN_ID), eq("Quality not met")))
                .thenReturn(rejectedDTO);

        RejectPayrollRequest request = new RejectPayrollRequest("Quality not met");

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/reject", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payroll rejected"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Quality not met"));
    }

    @Test
    void rejectPayroll_missingReason_returns400() throws Exception {
        RejectPayrollRequest request = new RejectPayrollRequest(null);

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/reject", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayroll_blankReason_returns400() throws Exception {
        RejectPayrollRequest request = new RejectPayrollRequest("   ");

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/reject", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayroll_notFound_returns404() throws Exception {
        RejectPayrollRequest request = new RejectPayrollRequest("Reason");
        when(pembayaranCommandUseCase.rejectPayroll(eq(PAYROLL_ID), eq(ADMIN_ID), any()))
                .thenThrow(new EntityNotFoundException("Payroll not found"));

        mockMvc.perform(post("/api/pembayaran/payroll/{payrollId}/reject", PAYROLL_ID)
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserWalletBalance_existingUser_returns200() throws Exception {
        WalletBalanceDTO balanceDTO = sampleWalletBalanceDTO();
        when(walletQueryUseCase.getUserWalletBalance(USER_ID)).thenReturn(balanceDTO);

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.balance").value(100000));
    }

    @Test
    void getUserWalletBalance_notFound_returns404() throws Exception {
        when(walletQueryUseCase.getUserWalletBalance(USER_ID))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWalletTransactions_existingUser_returns200WithList() throws Exception {
        List<WalletTransactionDTO> transactions = List.of(sampleWalletTransactionDTO());
        when(walletQueryUseCase.getWalletTransactions(USER_ID)).thenReturn(transactions);

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}/transactions", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value("CREDIT"));
    }

    @Test
    void getWalletTransactions_emptyList_returns200WithEmptyList() throws Exception {
        when(walletQueryUseCase.getWalletTransactions(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}/transactions", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void initiateTopUp_validRequest_returns200() throws Exception {
        TopUpResponseDTO responseDTO = sampleTopUpResponseDTO();
        when(pembayaranCommandUseCase.initiateTopUp(ADMIN_ID, 50000)).thenReturn(responseDTO);

        TopUpRequestDTO request = new TopUpRequestDTO(50000, "bank_transfer");

        mockMvc.perform(post("/api/pembayaran/wallet/top-up")
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentUrl").value("https://midtrans.url/payment"));
    }

    @Test
    void initiateTopUp_zeroAmount_returns400() throws Exception {
        when(pembayaranCommandUseCase.initiateTopUp(ADMIN_ID, 0))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        TopUpRequestDTO request = new TopUpRequestDTO(0, "bank_transfer");

        mockMvc.perform(post("/api/pembayaran/wallet/top-up")
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleMidtransCallback_validPayload_returns200() throws Exception {
        PaymentCallbackDTO callbackDTO = new PaymentCallbackDTO(
                "order-123",
                "order-123",
                "50000.00",
                "settlement",
                "bank_transfer",
                "200",
                "signature"
        );
        doNothing().when(pembayaranCommandUseCase).handlePaymentCallback(any());

        mockMvc.perform(post("/api/pembayaran/wallet/midtrans-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(pembayaranCommandUseCase).handlePaymentCallback(any());
    }

    @Test
    void handleMidtransCallback_illegalArgument_returns400() throws Exception {
        PaymentCallbackDTO callbackDTO = new PaymentCallbackDTO(
                "order-123",
                "order-123",
                "50000.00",
                "settlement",
                "bank_transfer",
                "200",
                "signature"
        );
        doThrow(new IllegalArgumentException("Invalid callback data"))
                .when(pembayaranCommandUseCase).handlePaymentCallback(any());

        mockMvc.perform(post("/api/pembayaran/wallet/midtrans-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDTO)))
                .andExpect(status().isBadRequest());
    }
}