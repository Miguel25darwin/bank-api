package com.bank.exception;

import java.math.BigDecimal;

/**
 * Levée quand le solde du compte est insuffisant pour effectuer un retrait.
 * Mappée sur {@code 400 Bad Request} par le {@link GlobalExceptionHandler}.
 */
public class InsufficientFundsException extends BankException {

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super("INSUFFICIENT_FUNDS",
                "Solde insuffisant. Disponible : " + available + " | Demandé : " + requested);
    }
}
