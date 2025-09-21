package kr.co._29cm.homework.exception;

/**
 * 주문을 찾을 수 없는 경우 발생하는 예외
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
public class OrderNotFoundException extends BusinessException {
    
    private static final String DEFAULT_CODE = "ORDER_NOT_FOUND";
    
    public OrderNotFoundException(String orderNumber) {
        super("주문번호 " + orderNumber + "인 주문을 찾을 수 없습니다", DEFAULT_CODE);
    }
    
    public OrderNotFoundException(String message, String code) {
        super(message, code);
    }
}
