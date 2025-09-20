package kr.co._29cm.homework.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 엔티티
 * 
 * 29CM의 상품 정보를 나타내는 도메인 엔티티입니다.
 * 상품번호, 상품명, 가격, 재고수량 등의 정보를 관리하며,
 * 주문 시 재고 차감 로직을 제공합니다.
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품 정보")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Column(name = "product_number", nullable = false, unique = true)
    @Schema(description = "상품번호", example = "768848")
    private Long productNumber;

    @Column(name = "name", nullable = false)
    @Schema(description = "상품명", example = "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    @Schema(description = "판매가격", example = "21000")
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Schema(description = "재고수량", example = "45")
    private Integer stockQuantity;

    @Version
    @Schema(description = "버전 (낙관적 락용)", example = "1")
    private Long version;

    /**
     * 상품 생성자
     * 
     * @param productNumber 상품번호
     * @param name 상품명
     * @param price 상품 가격 (precision=19, scale=2)
     * @param stockQuantity 재고 수량
     */
    public Product(Long productNumber, String name, BigDecimal price, Integer stockQuantity) {
        this.productNumber = productNumber;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.version = 0L; // 낙관적 락을 위한 버전 초기화
    }

    /**
     * 재고 차감
     * 
     * 주문 시 상품의 재고를 차감합니다.
     * 재고 부족 시 InsufficientStockException을 발생시킵니다.
     * 
     * @param quantity 차감할 수량 (0보다 커야 함)
     * @throws IllegalArgumentException 수량이 0 이하인 경우
     * @throws InsufficientStockException 재고가 부족한 경우
     */
    public void decreaseStock(int quantity) {
        // 수량 유효성 검증
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 0보다 커야 합니다.");
        }
        
        // 재고 부족 여부 확인
        int after = this.stockQuantity - quantity;
        if (after < 0) {
            throw new kr.co._29cm.homework.exception.InsufficientStockException(
                    this.productNumber, this.name, quantity, this.stockQuantity);
        }
        
        // 재고 차감
        this.stockQuantity = after;
    }
}


