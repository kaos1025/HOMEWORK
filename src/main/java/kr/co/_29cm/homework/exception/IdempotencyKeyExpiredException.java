package kr.co._29cm.homework.exception;

public class IdempotencyKeyExpiredException extends BusinessException {
    
    public IdempotencyKeyExpiredException(String idempotencyKey) {
        super("IDEMPOTENCY_KEY_EXPIRED", "Idempotency-Key가 만료되었습니다: " + idempotencyKey);
    }
}
