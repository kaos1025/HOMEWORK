package kr.co._29cm.homework.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_number", nullable = false, unique = true)
    private Long productNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

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


