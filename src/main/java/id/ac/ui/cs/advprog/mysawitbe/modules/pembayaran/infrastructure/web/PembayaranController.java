package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.RejectPayrollRequest;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpResponseDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.PembayaranQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.WalletQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pembayaran")
@RequiredArgsConstructor
public class PembayaranController {

	private final PembayaranQueryUseCase pembayaranQueryUseCase;
	private final PembayaranCommandUseCase pembayaranCommandUseCase;
	private final WalletQueryUseCase walletQueryUseCase;

	@GetMapping("/payroll/{payrollId}/status")
	public ResponseEntity<ApiResponse<PayrollStatusDTO>> getPayrollStatus(@PathVariable UUID payrollId) {
		PayrollStatusDTO result = pembayaranQueryUseCase.getPayrollStatus(payrollId);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@GetMapping("/payroll/user/{userId}")
	public ResponseEntity<ApiResponse<PayrollPageDTO>> getPayrollsByUserId(
			@PathVariable UUID userId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		PayrollPageDTO result = pembayaranQueryUseCase.getPayrollsByUserId(userId, startDate, endDate, status, page, size);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@GetMapping("/payroll")
	public ResponseEntity<ApiResponse<PayrollPageDTO>> listAllPayrolls(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		PayrollPageDTO result = pembayaranQueryUseCase.listAllPayrolls(startDate, endDate, status, page, size);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@PostMapping("/payroll/{payrollId}/approve")
	public ResponseEntity<ApiResponse<PayrollDTO>> approvePayroll(
			@PathVariable UUID payrollId,
			@RequestAttribute("userId") UUID adminId
	) {
		PayrollDTO result = pembayaranCommandUseCase.approvePayroll(payrollId, adminId);
		return ResponseEntity.ok(ApiResponse.success("Payroll approved", result));
	}

	@PostMapping("/payroll/{payrollId}/reject")
	public ResponseEntity<ApiResponse<PayrollDTO>> rejectPayroll(
			@PathVariable UUID payrollId,
			@RequestAttribute("userId") UUID adminId,
			@Valid @RequestBody RejectPayrollRequest request
	) {
		PayrollDTO result = pembayaranCommandUseCase.rejectPayroll(payrollId, adminId, request.reason());
		return ResponseEntity.ok(ApiResponse.success("Payroll rejected", result));
	}

	@GetMapping("/wallet/{userId}")
	public ResponseEntity<ApiResponse<WalletBalanceDTO>> getUserWalletBalance(@PathVariable UUID userId) {
		WalletBalanceDTO result = walletQueryUseCase.getUserWalletBalance(userId);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@GetMapping("/wallet/{userId}/transactions")
	public ResponseEntity<ApiResponse<List<WalletTransactionDTO>>> getWalletTransactions(@PathVariable UUID userId) {
		List<WalletTransactionDTO> result = walletQueryUseCase.getWalletTransactions(userId);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@PostMapping("/wallet/top-up")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<TopUpResponseDTO>> initiateTopUp(
			@Valid @RequestBody TopUpRequestDTO request,
			@RequestAttribute("userId") UUID adminId
	) {
		TopUpResponseDTO result = pembayaranCommandUseCase.initiateTopUp(adminId, request.amount());
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@PostMapping("/wallet/midtrans-callback")
	public ResponseEntity<ApiResponse<Void>> handleMidtransCallback(
			@RequestBody PaymentCallbackDTO payload
	) {
		pembayaranCommandUseCase.handlePaymentCallback(payload);
		return ResponseEntity.ok(ApiResponse.success(null));
	}
}
