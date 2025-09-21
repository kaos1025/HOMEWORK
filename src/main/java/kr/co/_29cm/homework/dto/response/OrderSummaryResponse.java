package kr.co._29cm.homework.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 요약 응답 DTO
 * 
 * 주문 목록 조회 시 사용되는 간략한 주문 정보를 제공합니다.
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Getter
@AllArgsConstructor
@Schema(description = "주문 요약 정보")
public class OrderSummaryResponse {
    
    @Schema(description = "주문번호", example = "550e8400-e29b-41d4-a716-446655440000")
    private String orderNumber;
    
    @Schema(description = "주문일시", example = "2025-01-19T12:00:00")
    private LocalDateTime orderedAt;
    
    @Schema(description = "주문 상품 개수", example = "3")
    private int itemCount;
    
    @Schema(description = "총 결제금액", example = "29100")
    private BigDecimal totalPayment;
}
