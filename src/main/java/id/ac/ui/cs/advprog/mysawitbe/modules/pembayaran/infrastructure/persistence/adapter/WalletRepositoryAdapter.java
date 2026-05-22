package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.common.domain.Money;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.WalletRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
	@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
			maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
	public WalletBalanceDTO credit(UUID userId, long amount, UUID payrollId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Credit amount must be positive");
		}

		Money moneyAmount = Money.of(amount);
		WalletEntity wallet = findOrCreateWallet(userId);
		wallet.setBalance(wallet.getBalance().add(moneyAmount));
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(payrollId)
				.amount(moneyAmount)
				.type(CREDIT_TYPE)
				.build();
		walletTransactionJpaRepository.save(transaction);

		return walletMapper.toBalanceDto(savedWallet);
	}

	@Override
	@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
			maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
	public WalletBalanceDTO debit(UUID userId, long amount, UUID payrollId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Debit amount must be positive");
		}

		Money moneyAmount = Money.of(amount);
		WalletEntity wallet = findOrCreateWallet(userId);
		if (wallet.getBalance().isLessThan(moneyAmount)) {
			throw new IllegalStateException("Insufficient admin wallet balance");
		}
		wallet.setBalance(wallet.getBalance().subtract(moneyAmount));
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(payrollId)
				.amount(moneyAmount)
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
	@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
			maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
	public WalletBalanceDTO creditTopUp(UUID userId, long amount, String reference) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Credit amount must be positive");
		}

		if (reference != null && walletTransactionJpaRepository.existsByReference(reference)) {
			return walletMapper.toBalanceDto(findOrCreateWallet(userId));
		}

		Money moneyAmount = Money.of(amount);
		WalletEntity wallet = findOrCreateWallet(userId);
		wallet.setBalance(wallet.getBalance().add(moneyAmount));
		WalletEntity savedWallet = walletJpaRepository.save(wallet);

		WalletTransactionEntity transaction = WalletTransactionEntity.builder()
				.userId(userId)
				.payrollId(null)
				.amount(moneyAmount)
				.type(CREDIT_TYPE)
				.reference(reference)
				.build();
		walletTransactionJpaRepository.save(transaction);

		return walletMapper.toBalanceDto(savedWallet);
	}

	@Recover
	public WalletBalanceDTO recoverFromOptimisticLock(ObjectOptimisticLockingFailureException ex,
			UUID userId, long amount, UUID payrollId) {
		throw new IllegalStateException("Wallet operation failed after retries due to concurrent modification.");
	}

	@Recover
	public WalletBalanceDTO recoverFromOptimisticLockTopUp(ObjectOptimisticLockingFailureException ex,
			UUID userId, long amount, String reference) {
		throw new IllegalStateException("Wallet top-up failed after retries due to concurrent modification.");
	}

	private WalletEntity findOrCreateWallet(UUID userId) {
		return walletJpaRepository.findById(userId)
				.orElseGet(() -> walletJpaRepository.save(
						WalletEntity.builder()
								.userId(userId)
								.balance(Money.ZERO)
								.build()
				));
	}
}
