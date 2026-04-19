package com.bank.service;

import com.bank.dto.AccountRequest;
import com.bank.dto.AccountResponse;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResponse;
import com.bank.exception.AccountNotActiveException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.EmailAlreadyExistsException;
import com.bank.exception.InsufficientFundsException;
import com.bank.model.Account;
import com.bank.model.Account.AccountStatus;
import com.bank.model.Account.AccountType;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link AccountService}.
 * Utilise Mockito pour isoler la couche service des repositories.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService — Tests Unitaires")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account activeAccount;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        activeAccount = Account.builder()
                .id(accountId)
                .ownerName("Jean Dupont")
                .email("jean.dupont@example.com")
                .accountNumber("ACC-ABCD1234")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ================================================================
    // createAccount
    // ================================================================

    @Test
    @DisplayName("createAccount — succès : compte créé et retourné")
    void createAccount_success() {
        // Arrange
        AccountRequest request = new AccountRequest();
        request.setOwnerName("Jean Dupont");
        request.setEmail("jean.dupont@example.com");
        request.setAccountType(AccountType.CHECKING);
        request.setCurrency("EUR");

        when(accountRepository.existsByEmail("jean.dupont@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

        // Act
        AccountResponse response = accountService.createAccount(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOwnerName()).isEqualTo("Jean Dupont");
        assertThat(response.getEmail()).isEqualTo("jean.dupont@example.com");
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccount — email déjà existant → EmailAlreadyExistsException")
    void createAccount_emailAlreadyExists_throwsException() {
        // Arrange
        AccountRequest request = new AccountRequest();
        request.setOwnerName("Jean Dupont");
        request.setEmail("jean.dupont@example.com");
        request.setAccountType(AccountType.CHECKING);

        when(accountRepository.existsByEmail("jean.dupont@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("jean.dupont@example.com");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAccount — currency null → forcée à EUR")
    void createAccount_nullCurrency_defaultsToEur() {
        // Arrange
        AccountRequest request = new AccountRequest();
        request.setOwnerName("Jean Dupont");
        request.setEmail("jean.dupont@example.com");
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency(null);

        Account savedWithEur = Account.builder()
                .id(UUID.randomUUID())
                .ownerName("Jean Dupont")
                .email("jean.dupont@example.com")
                .accountNumber("ACC-XXXXXXXX")
                .accountType(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountRepository.existsByEmail(any())).thenReturn(false);
        when(accountRepository.save(any())).thenReturn(savedWithEur);

        // Act
        AccountResponse response = accountService.createAccount(request);

        // Assert
        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    // ================================================================
    // getAllAccounts
    // ================================================================

    @Test
    @DisplayName("getAllAccounts — retourne la liste complète")
    void getAllAccounts_returnsAllAccounts() {
        // Arrange
        Account second = Account.builder()
                .id(UUID.randomUUID())
                .ownerName("Marie Martin")
                .email("marie@example.com")
                .accountNumber("ACC-EFGH5678")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(activeAccount, second));

        // Act
        List<AccountResponse> accounts = accountService.getAllAccounts();

        // Assert
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(AccountResponse::getEmail)
                .containsExactlyInAnyOrder("jean.dupont@example.com", "marie@example.com");
    }

    // ================================================================
    // getAccountById
    // ================================================================

    @Test
    @DisplayName("getAccountById — compte trouvé → retourne DTO")
    void getAccountById_found_returnsDto() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));

        AccountResponse response = accountService.getAccountById(accountId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("getAccountById — compte introuvable → AccountNotFoundException")
    void getAccountById_notFound_throwsException() {
        UUID unknown = UUID.randomUUID();
        when(accountRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(unknown))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    // ================================================================
    // deposit
    // ================================================================

    @Test
    @DisplayName("deposit — succès : solde augmenté de 200")
    void deposit_success_balanceIncreased() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Virement mensuel");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any())).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        AccountResponse response = accountService.deposit(accountId, request);

        // Assert
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
        verify(transactionRepository).save(argThat(tx -> tx.getType() == Transaction.TransactionType.CREDIT
                && tx.getAmount().compareTo(new BigDecimal("200.00")) == 0));
    }

    @Test
    @DisplayName("deposit — compte inactif → AccountNotActiveException")
    void deposit_suspendedAccount_throwsException() {
        activeAccount.setStatus(AccountStatus.SUSPENDED);
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> accountService.deposit(accountId, request))
                .isInstanceOf(AccountNotActiveException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // ================================================================
    // withdraw
    // ================================================================

    @Test
    @DisplayName("withdraw — succès : solde diminué de 300")
    void withdraw_success_balanceDecreased() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("300.00"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any())).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        AccountResponse response = accountService.withdraw(accountId, request);

        // Assert
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(transactionRepository).save(argThat(tx -> tx.getType() == Transaction.TransactionType.DEBIT));
    }

    @Test
    @DisplayName("withdraw — solde insuffisant → InsufficientFundsException")
    void withdraw_insufficientFunds_throwsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("5000.00")); // supérieur au solde de 1000

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));

        // Act & Assert
        assertThatThrownBy(() -> accountService.withdraw(accountId, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("1000.00")
                .hasMessageContaining("5000.00");

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("withdraw — compte clôturé → AccountNotActiveException")
    void withdraw_closedAccount_throwsException() {
        activeAccount.setStatus(AccountStatus.CLOSED);
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> accountService.withdraw(accountId, request))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessageContaining("CLOSED");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("withdraw — retrait exactement égal au solde → succès")
    void withdraw_exactBalance_success() {
        // Arrange : retrait égal au solde disponible
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("1000.00"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any())).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenReturn(null);

        // Act
        AccountResponse response = accountService.withdraw(accountId, request);

        // Assert
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ================================================================
    // getTransactionHistory
    // ================================================================

    @Test
    @DisplayName("getTransactionHistory — retourne la page de transactions")
    void getTransactionHistory_returnsPaginatedTransactions() {
        // Arrange
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .account(activeAccount)
                .type(Transaction.TransactionType.CREDIT)
                .amount(new BigDecimal("200.00"))
                .balanceAfter(new BigDecimal("1200.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> txPage = new PageImpl<>(List.of(tx), pageable, 1);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount));
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable))
                .thenReturn(txPage);

        // Act
        Page<TransactionResponse> result = accountService.getTransactionHistory(accountId, pageable);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo("CREDIT");
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("getTransactionHistory — compte introuvable → AccountNotFoundException")
    void getTransactionHistory_unknownAccount_throwsException() {
        UUID unknown = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(accountRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getTransactionHistory(unknown, pageable))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
