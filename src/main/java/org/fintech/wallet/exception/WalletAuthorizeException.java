package org.fintech.wallet.exception;

public class WalletAuthorizeException extends RuntimeException {
    public WalletAuthorizeException(String message) {
        super(message);
    }
}