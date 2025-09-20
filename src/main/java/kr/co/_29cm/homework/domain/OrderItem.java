package kr.co._29cm.homework.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 아이템 엔티티
 * 
 * 주문에 포함된 개별 상품 정보를 나타내는 엔티티입니다.
 * 주문 시점의 상품 정보를 스냅샷으로 저장하여 데이터 일관성을 보장합니다.
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_number", nullable = false)
    private Long productNumber;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 주문 아이템 생성자
     * 
     * 주문 시점의 상품 정보를 스냅샷으로 저장합니다.
     * 상품 정보가 변경되어도 주문 당시의 정보가 보존됩니다.
     * 
     * @param product 주문할 상품
     * @param quantity 주문 수량
     */
    public OrderItem(Product product, Integer quantity) {
        this.product = product;
        this.productNumber = product.getProductNumber();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    /**
     * 주문과의 양방향 연관관계 설정
     * 
     * @param order 연결할 주문
     */
    void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 주문 아이템 총 가격 계산
     * 
     * 단가 × 수량을 계산하여 총 가격을 반환합니다.
     * HALF_UP 라운딩을 적용하여 소수점 둘째 자리까지 반환합니다.
     * 
     * @return 주문 아이템 총 가격 (HALF_UP 라운딩 적용)
     */
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}


