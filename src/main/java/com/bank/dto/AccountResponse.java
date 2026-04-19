package com.bank.dto;

import com.bank.model.Account;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private String ownerName;
    private String email;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private Account.AccountStatus status;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Méthode de mapping depuis l'entité
    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .email(account.getEmail())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}