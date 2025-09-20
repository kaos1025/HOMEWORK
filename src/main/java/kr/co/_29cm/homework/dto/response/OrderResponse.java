package kr.co._29cm.homework.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 응답")
public class OrderResponse {

    @Schema(description = "주문번호", example = "550e8400-e29b-41d4-a716-446655440000")
    private String orderNumber;

    @Schema(description = "주문일시", example = "2025-01-19T12:00:00")
    private LocalDateTime orderedAt;

    @Schema(description = "주문항목 목록")
    private List<OrderItemResponse> items;

    @Schema(description = "지불금액 (총 주문금액 + 배송비)", example = "26600")
    private BigDecimal paymentAmount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "주문 항목 응답")
    public static class OrderItemResponse {

        @Schema(description = "상품번호", example = "768848")
        private Long productNumber;

        @Schema(description = "상품명", example = "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
        private String productName;

        @Schema(description = "주문수량", example = "1")
        private Integer quantity;

        @Schema(description = "단가", example = "21000")
        private BigDecimal unitPrice;

        @Schema(description = "소계 (단가 × 수량)", example = "21000")
        private BigDecimal subtotal;
    }
}
