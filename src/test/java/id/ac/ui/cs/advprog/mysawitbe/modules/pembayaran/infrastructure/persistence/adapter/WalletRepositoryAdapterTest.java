package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletBalanceDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.WalletTransactionDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.WalletTransactionJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletRepositoryAdapterTest {

    @Mock
    private WalletJpaRepository walletJpaRepository;

    @Mock
    private WalletTransactionJpaRepository walletTransactionJpaRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletRepositoryAdapter adapter;

    private UUID userId;
    private UUID payrollId;
    private WalletEntity walletEntity;
    private WalletBalanceDTO walletBalanceDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        payrollId = UUID.randomUUID();
        walletEntity = WalletEntity.builder()
                .userId(userId)
                .balance(100000)
                .updatedAt(LocalDateTime.now())
                .build();
        walletBalanceDto = new WalletBalanceDTO(userId, 100000, LocalDateTime.now());
    }

    @Nested
    class FindBalanceByUserId {

        @Test
        void whenWalletExists_returnsBalance() {
            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletMapper.toBalanceDto(walletEntity)).thenReturn(walletBalanceDto);

            WalletBalanceDTO result = adapter.findBalanceByUserId(userId);

            assertThat(result).isEqualTo(walletBalanceDto);
            verify(walletJpaRepository).findById(userId);
            verify(walletMapper).toBalanceDto(walletEntity);
        }

        @Test
        void whenWalletDoesNotExist_createsWalletWithZeroBalance() {
            WalletEntity newWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(0)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO newBalanceDto = new WalletBalanceDTO(userId, 0, LocalDateTime.now());

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.empty());
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(newWallet);
            when(walletMapper.toBalanceDto(newWallet)).thenReturn(newBalanceDto);

            WalletBalanceDTO result = adapter.findBalanceByUserId(userId);

            assertThat(result.balance()).isEqualTo(0);
            verify(walletJpaRepository).findById(userId);
            ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getBalance()).isEqualTo(0);
        }
    }

    @Nested
    class Credit {

        @Test
        void positiveAmount_walletBalanceIncreasesAndTransactionCreated() {
            int creditAmount = 50000;
            WalletEntity updatedWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(150000)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, 150000, LocalDateTime.now());

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(updatedWallet);
            when(walletMapper.toBalanceDto(updatedWallet)).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.credit(userId, creditAmount, payrollId);

            assertThat(result.balance()).isEqualTo(150000);
            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualTo(150000);

            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            WalletTransactionEntity savedTx = txCaptor.getValue();
            assertThat(savedTx.getAmount()).isEqualTo(creditAmount);
            assertThat(savedTx.getType()).isEqualTo("CREDIT");
            assertThat(savedTx.getPayrollId()).isEqualTo(payrollId);
            assertThat(savedTx.getUserId()).isEqualTo(userId);
        }

        @Test
        void zeroAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.credit(userId, 0, payrollId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit amount must be positive");
        }

        @Test
        void negativeAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.credit(userId, -10000, payrollId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit amount must be positive");
        }

        @Test
        void positiveAmountWhenWalletNotFound_createsWalletAndCredits() {
            int creditAmount = 50000;
            WalletEntity newWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(creditAmount)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, creditAmount, LocalDateTime.now());

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.empty());
            when(walletJpaRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(walletMapper.toBalanceDto(any(WalletEntity.class))).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.credit(userId, creditAmount, payrollId);

            assertThat(result.balance()).isEqualTo(creditAmount);
            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository, atLeast(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualTo(creditAmount);

            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            WalletTransactionEntity savedTx = txCaptor.getValue();
            assertThat(savedTx.getAmount()).isEqualTo(creditAmount);
            assertThat(savedTx.getType()).isEqualTo("CREDIT");
            assertThat(savedTx.getUserId()).isEqualTo(userId);
            assertThat(savedTx.getPayrollId()).isEqualTo(payrollId);
        }
    }

    @Nested
    class Debit {

        @Test
        void positiveAmountWithSufficientBalance_walletBalanceDecreasesAndTransactionCreated() {
            int debitAmount = 30000;
            WalletEntity updatedWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(70000)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, 70000, LocalDateTime.now());

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(updatedWallet);
            when(walletMapper.toBalanceDto(updatedWallet)).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.debit(userId, debitAmount, payrollId);

            assertThat(result.balance()).isEqualTo(70000);
            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualTo(70000);

            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            WalletTransactionEntity savedTx = txCaptor.getValue();
            assertThat(savedTx.getAmount()).isEqualTo(debitAmount);
            assertThat(savedTx.getType()).isEqualTo("DEBIT");
            assertThat(savedTx.getPayrollId()).isEqualTo(payrollId);
            assertThat(savedTx.getUserId()).isEqualTo(userId);
        }

        @Test
        void positiveAmountWithInsufficientBalance_throwsIllegalStateException() {
            int debitAmount = 150000;
            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));

            assertThatThrownBy(() -> adapter.debit(userId, debitAmount, payrollId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient admin wallet balance");

            verify(walletJpaRepository, never()).save(any());
            verify(walletTransactionJpaRepository, never()).save(any());
        }

        @Test
        void positiveAmountWithWalletNotFound_createsWalletThenThrowsInsufficientBalance() {
            int debitAmount = 30000;
            WalletEntity newWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(0)
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.empty());
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(newWallet);

            assertThatThrownBy(() -> adapter.debit(userId, debitAmount, payrollId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient admin wallet balance");

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository, times(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualTo(0);
        }

        @Test
        void zeroAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.debit(userId, 0, payrollId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Debit amount must be positive");
        }

        @Test
        void negativeAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.debit(userId, -5000, payrollId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Debit amount must be positive");
        }
    }

    @Nested
    class FindTransactionsByUserId {

        @Test
        void returnsTransactionsOrderedByCreatedAtDesc() {
            WalletTransactionEntity tx1 = WalletTransactionEntity.builder()
                    .transactionId(UUID.randomUUID())
                    .userId(userId)
                    .amount(10000)
                    .type("CREDIT")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();
            WalletTransactionEntity tx2 = WalletTransactionEntity.builder()
                    .transactionId(UUID.randomUUID())
                    .userId(userId)
                    .amount(20000)
                    .type("DEBIT")
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();

            List<WalletTransactionEntity> transactions = List.of(tx2, tx1);
            List<WalletTransactionDTO> dtos = List.of(
                    new WalletTransactionDTO(tx2.getTransactionId(), userId, null, 20000, "DEBIT", null, tx2.getCreatedAt()),
                    new WalletTransactionDTO(tx1.getTransactionId(), userId, null, 10000, "CREDIT", null, tx1.getCreatedAt())
            );

            when(walletTransactionJpaRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(transactions);
            when(walletMapper.toTransactionDtoList(transactions)).thenReturn(dtos);

            List<WalletTransactionDTO> result = adapter.findTransactionsByUserId(userId);

            assertThat(result).hasSize(2);
            verify(walletTransactionJpaRepository).findByUserIdOrderByCreatedAtDesc(userId);
            verify(walletMapper).toTransactionDtoList(transactions);
        }

        @Test
        void returnsEmptyListWhenNoTransactions() {
            when(walletTransactionJpaRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(Collections.emptyList());
            when(walletMapper.toTransactionDtoList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            List<WalletTransactionDTO> result = adapter.findTransactionsByUserId(userId);

            assertThat(result).isEmpty();
            verify(walletMapper).toTransactionDtoList(Collections.emptyList());
        }
    }

    @Nested
    class CreditTopUp {

        @Test
        void newReference_creditsWalletAndCreatesTransactionWithReference() {
            int amount = 50000;
            String reference = "TOPUP:admin-123";
            WalletEntity updatedWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(150000)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, 150000, LocalDateTime.now());

            when(walletTransactionJpaRepository.existsByReference(reference)).thenReturn(false);
            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(updatedWallet);
            when(walletMapper.toBalanceDto(updatedWallet)).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.creditTopUp(userId, amount, reference);

            assertThat(result.balance()).isEqualTo(150000);
            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            WalletTransactionEntity savedTx = txCaptor.getValue();
            assertThat(savedTx.getReference()).isEqualTo(reference);
            assertThat(savedTx.getAmount()).isEqualTo(amount);
            assertThat(savedTx.getType()).isEqualTo("CREDIT");
            assertThat(savedTx.getPayrollId()).isNull();
        }

        @Test
        void existingReference_returnsIdempotentWithoutCreditingAgain() {
            int amount = 50000;
            String reference = "TOPUP:admin-123";
            WalletBalanceDTO existingBalanceDto = new WalletBalanceDTO(userId, 100000, LocalDateTime.now());

            when(walletTransactionJpaRepository.existsByReference(reference)).thenReturn(true);
            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletMapper.toBalanceDto(walletEntity)).thenReturn(existingBalanceDto);

            WalletBalanceDTO result = adapter.creditTopUp(userId, amount, reference);

            assertThat(result.balance()).isEqualTo(100000);
            verify(walletJpaRepository, never()).save(any());
            verify(walletTransactionJpaRepository, never()).save(any());
        }

        @Test
        void zeroAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.creditTopUp(userId, 0, "ref"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit amount must be positive");
        }

        @Test
        void negativeAmount_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.creditTopUp(userId, -10000, "ref"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit amount must be positive");
        }

        @Test
        void nullReference_createsTransactionWithoutReferenceCheck() {
            int amount = 50000;
            WalletEntity updatedWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(150000)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, 150000, LocalDateTime.now());

            when(walletJpaRepository.findById(userId)).thenReturn(Optional.of(walletEntity));
            when(walletJpaRepository.save(any(WalletEntity.class))).thenReturn(updatedWallet);
            when(walletMapper.toBalanceDto(updatedWallet)).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.creditTopUp(userId, amount, null);

            assertThat(result.balance()).isEqualTo(150000);
            verify(walletTransactionJpaRepository, never()).existsByReference(any());
            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getReference()).isNull();
        }

        @Test
        void newReferenceWhenWalletNotFound_createsWalletAndCredits() {
            int amount = 50000;
            String reference = "TOPUP:admin-456";
            WalletEntity newWallet = WalletEntity.builder()
                    .userId(userId)
                    .balance(amount)
                    .updatedAt(LocalDateTime.now())
                    .build();
            WalletBalanceDTO expectedDto = new WalletBalanceDTO(userId, amount, LocalDateTime.now());

            when(walletTransactionJpaRepository.existsByReference(reference)).thenReturn(false);
            when(walletJpaRepository.findById(userId)).thenReturn(Optional.empty());
            when(walletJpaRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(walletMapper.toBalanceDto(any(WalletEntity.class))).thenReturn(expectedDto);

            WalletBalanceDTO result = adapter.creditTopUp(userId, amount, reference);

            assertThat(result.balance()).isEqualTo(amount);
            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            verify(walletJpaRepository, atLeast(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualTo(amount);

            ArgumentCaptor<WalletTransactionEntity> txCaptor = ArgumentCaptor.forClass(WalletTransactionEntity.class);
            verify(walletTransactionJpaRepository).save(txCaptor.capture());
            WalletTransactionEntity savedTx = txCaptor.getValue();
            assertThat(savedTx.getReference()).isEqualTo(reference);
            assertThat(savedTx.getAmount()).isEqualTo(amount);
            assertThat(savedTx.getType()).isEqualTo("CREDIT");
            assertThat(savedTx.getPayrollId()).isNull();
        }
    }
}