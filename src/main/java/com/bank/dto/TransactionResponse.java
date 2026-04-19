package com.bank.dto;

import com.bank.model.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une transaction.
 * Expose uniquement les informations nécessaires (pas les entités JPA).
 * Inclut l'identifiant du compte pour permettre au client de faire le lien.
 */
@Data
@Builder
public class TransactionResponse {

    private UUID id;
    /** Identifiant du compte associé à cette transaction. */
    private UUID accountId;
    private String type; // "CREDIT" ou "DEBIT"
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String reference;
    private LocalDateTime createdAt;

    /**
     * Factory method : convertit une entité Transaction en DTO.
     * Évite d'exposer les entités JPA directement dans la couche HTTP.
     *
     * @param tx l'entité Transaction persistée
     * @return le DTO correspondant
     */
    public static TransactionResponse from(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .accountId(tx.getAccount().getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .reference(tx.getReference())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
