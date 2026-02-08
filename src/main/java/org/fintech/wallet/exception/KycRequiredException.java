package org.fintech.wallet.exception;

public class KycRequiredException extends RuntimeException {
    public KycRequiredException(String message) {
        super(message);
    }
}
