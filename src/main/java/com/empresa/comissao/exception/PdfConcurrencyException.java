package com.empresa.comissao.exception;

/**
 * Lançada quando o limite de PDFs simultâneos é atingido.
 * Produz HTTP 429 Too Many Requests com header Retry-After.
 */
public class PdfConcurrencyException extends RuntimeException {

    private final int retryAfterSeconds;

    public PdfConcurrencyException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
