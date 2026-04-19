package com.bank.exception;

import com.bank.model.Account;

/**
 * Levée quand une transaction est tentée sur un compte non actif (suspendu ou
 * clôturé).
 * Mappée sur {@code 403 Forbidden} par le {@link GlobalExceptionHandler}.
 */
public class AccountNotActiveException extends BankException {

    public AccountNotActiveException(String accountNumber, Account.AccountStatus status) {
        super("ACCOUNT_NOT_ACTIVE",
                "Le compte " + accountNumber + " est " + status
                        + " et ne peut pas effectuer de transactions.");
    }
}
