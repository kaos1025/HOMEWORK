package kr.co._29cm.homework.exception;

public class DuplicateRequestException extends BusinessException {
    
    public DuplicateRequestException(String idempotencyKey) {
        super("DUPLICATE_REQUEST", "중복된 요청입니다. Idempotency-Key: " + idempotencyKey);
    }

    public DuplicateRequestException(String idempotencyKey, String message) {
        super("DUPLICATE_REQUEST", message + " Idempotency-Key: " + idempotencyKey);
    }
}
