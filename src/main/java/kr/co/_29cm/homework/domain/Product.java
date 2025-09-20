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

    public Product(Long productNumber, String name, BigDecimal price, Integer stockQuantity) {
        this.productNumber = productNumber;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 0보다 커야 합니다.");
        }
        
        int after = this.stockQuantity - quantity;
        if (after < 0) {
            throw new kr.co._29cm.homework.exception.InsufficientStockException(
                    this.productNumber, this.name, quantity, this.stockQuantity);
        }
        this.stockQuantity = after;
    }
}


