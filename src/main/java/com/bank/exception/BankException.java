package com.bank.exception;

/**
 * Exception de base pour toutes les erreurs métier de l'application Bank API.
 *
 * <p>
 * Toutes les exceptions fonctionnelles héritent de cette classe, ce qui permet
 * au {@link GlobalExceptionHandler} d'attraper précisément les erreurs métier
 * sans attraper des {@code RuntimeException} non prévues (bugs, NPE, etc.).
 * </p>
 */
public class BankException extends RuntimeException {

    private final String errorCode;

    public BankException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
