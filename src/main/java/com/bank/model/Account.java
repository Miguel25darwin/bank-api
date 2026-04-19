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
 * Entité JPA représentant un compte bancaire.
 * Utilise des enums internes pour garantir la cohérence des types.
 */
@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    // =============================================
    // CLÉ PRIMAIRE — UUID généré automatiquement
    // =============================================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // =============================================
    // INFORMATIONS DU TITULAIRE
    // =============================================
    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, unique = true)
    private String email;

    // Généré automatiquement dans @PrePersist
    @Column(nullable = false, unique = true, updatable = false)
    private String accountNumber;

    // =============================================
    // CONFIGURATION DU COMPTE
    // =============================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Solde stocké avec une précision haute pour éviter les erreurs d'arrondi.
     * Utilise BigDecimal (jamais float/double pour l'argent).
     */
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    // =============================================
    // HORODATAGE AUTOMATIQUE
    // =============================================
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Callback JPA : initialise les timestamps et génère le numéro de compte
     * lors de la première persistance. Garantit l'unicité sans dépendance externe.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.accountNumber == null) {
            // Format : ACC-XXXXXXXX (8 caractères hexadécimaux en majuscules)
            this.accountNumber = "ACC-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        }
    }

    /**
     * Callback JPA : met à jour le timestamp à chaque modification.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // =============================================
    // ENUMS MÉTIER (encapsulés dans l'entité)
    // =============================================

    /**
     * Type de compte bancaire.
     */
    public enum AccountType {
        SAVINGS, // Compte épargne
        CHECKING, // Compte courant
        BUSINESS // Compte professionnel
    }

    /**
     * Statut opérationnel du compte.
     * Seul un compte ACTIVE peut effectuer des transactions.
     */
    public enum AccountStatus {
        ACTIVE, // Compte opérationnel
        SUSPENDED, // Compte temporairement suspendu
        CLOSED // Compte définitivement clôturé
    }
}
