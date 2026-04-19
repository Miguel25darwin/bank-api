package com.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA représentant un mouvement financier (dépôt ou retrait).
 * Chaque transaction est immuable après création (pas d'update).
 */
@Entity
@Table(name = "transactions", indexes = {
        // Index composite pour accélérer les requêtes d'historique par compte
        @Index(name = "idx_transaction_account_date", columnList = "account_id, createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    // =============================================
    // CLÉ PRIMAIRE
    // =============================================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // =============================================
    // RELATION AVEC LE COMPTE (Many-to-One)
    // =============================================
    /**
     * Chargement LAZY pour éviter les requêtes N+1.
     * Jointure sur la colonne account_id (FK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    // =============================================
    // DONNÉES FINANCIÈRES
    // =============================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /**
     * Montant de la transaction. Toujours positif.
     * Le type (CREDIT/DEBIT) détermine le sens du mouvement.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Solde du compte immédiatement après cette transaction.
     * Stocké pour faciliter les audits sans recalcul.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    // =============================================
    // MÉTADONNÉES OPTIONNELLES
    // =============================================
    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String reference;

    // =============================================
    // HORODATAGE (immutable)
    // =============================================
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =============================================
    // ENUM MÉTIER
    // =============================================

    /**
     * Direction du flux financier.
     */
    public enum TransactionType {
        CREDIT, // Dépôt — augmente le solde
        DEBIT // Retrait — diminue le solde
    }
}
