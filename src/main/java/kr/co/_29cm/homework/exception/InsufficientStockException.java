package kr.co._29cm.homework.exception;

public class InsufficientStockException extends BusinessException {
    
    public InsufficientStockException(Long productNumber, String productName, int requestedQuantity, int availableStock) {
        super("INSUFFICIENT_STOCK", 
              String.format("재고가 부족합니다. 상품: %s(%d), 요청수량: %d, 재고: %d", 
                          productName, productNumber, requestedQuantity, availableStock));
    }
}
