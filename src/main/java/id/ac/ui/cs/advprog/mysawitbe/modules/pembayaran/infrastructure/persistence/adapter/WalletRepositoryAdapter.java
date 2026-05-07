package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.WalletRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepositoryPort {

	private static final String CREDIT_TYPE = "CREDIT";
	private static final String DEBIT_TYPE = "DEBIT";

	private final WalletJpaRepository walletJpaRepository;
	private final WalletTransactionJpaRepository walletTransactionJpaRepository;
	private final WalletMapper walletMapper;

	@Override
	public WalletBalanceDTO findBalanceByUserId(UUID userId) {
		return walletMapper.toBalanceDto(findOrCreateWallet(userId));
	}

	@Override
	public WalletBalanceDTO credit(UUID userId, int amount, UUID payrollId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Credit amount must be positive");
		}

		WalletEntity wallet = findOrCreateWallet(userId);
		wallet.setBalance(wallet.getBalance() + amount);
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(payrollId)
				.amount(amount)
				.type(CREDIT_TYPE)
				.build();
		walletTransactionJpaRepository.save(transaction);

		return walletMapper.toBalanceDto(savedWallet);
	}

	@Override
	public WalletBalanceDTO debit(UUID userId, int amount, UUID payrollId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Debit amount must be positive");
		}

		WalletEntity wallet = findOrCreateWallet(userId);
		if (wallet.getBalance() < amount) {
			throw new IllegalStateException("Insufficient admin wallet balance");
		}

		wallet.setBalance(wallet.getBalance() - amount);
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(payrollId)
				.amount(amount)
				.type(DEBIT_TYPE)
				.build();
		walletTransactionJpaRepository.save(transaction);

		return walletMapper.toBalanceDto(savedWallet);
	}

	@Override
	public List<WalletTransactionDTO> findTransactionsByUserId(UUID userId) {
		return walletMapper.toTransactionDtoList(
				walletTransactionJpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
		);
	}

	@Override
	public WalletBalanceDTO creditTopUp(UUID userId, int amount, String reference) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Credit amount must be positive");
		}

		if (reference != null && walletTransactionJpaRepository.existsByReference(reference)) {
			// Idempotency check: Topup already processed for this reference
			return walletMapper.toBalanceDto(findOrCreateWallet(userId));
		}

		WalletEntity wallet = findOrCreateWallet(userId);
		wallet.setBalance(wallet.getBalance() + amount);
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(null)
				.amount(amount)
				.type(CREDIT_TYPE)
				.reference(reference)
				.build();
		walletTransactionJpaRepository.save(transaction);

		return walletMapper.toBalanceDto(savedWallet);
	}

	private WalletEntity findOrCreateWallet(UUID userId) {
		return walletJpaRepository.findById(userId)
				.orElseGet(() -> walletJpaRepository.save(
						WalletEntity.builder()
								.userId(userId)
								.balance(0)
								.build()
				));
	}
}
