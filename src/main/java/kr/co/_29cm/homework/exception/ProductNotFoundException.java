package kr.co._29cm.homework.exception;

public class ProductNotFoundException extends BusinessException {
    
    public ProductNotFoundException(Long productNumber) {
        super("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다. 상품번호: " + productNumber);
    }
}
