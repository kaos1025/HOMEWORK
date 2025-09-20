package kr.co._29cm.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 관리", description = "상품 주문 API")
public class OrderController {

    private final OrderService orderService;

    @Schema(description = "주문 항목 요청")
    public record OrderRequestItem(
            @Schema(description = "상품번호", example = "768848")
            @NotNull Long productNumber, 
            @Schema(description = "주문수량", example = "1", minimum = "1")
            @NotNull @Min(1) Integer quantity
    ) {}
    
    @Schema(description = "주문 요청")
    public record OrderRequest(@Valid List<OrderRequestItem> items) {}

    @Schema(description = "주문 항목 응답")
    public record OrderItemResponse(
            @Schema(description = "상품번호", example = "768848")
            Long productNumber, 
            @Schema(description = "상품명", example = "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
            String productName, 
            @Schema(description = "주문수량", example = "1")
            Integer quantity, 
            @Schema(description = "단가", example = "21000")
            BigDecimal unitPrice
    ) {}
    
    @Schema(description = "주문 응답")
    public record OrderResponse(
            @Schema(description = "주문번호", example = "550e8400-e29b-41d4-a716-446655440000")
            String orderNumber, 
            @Schema(description = "주문일시", example = "2025-01-19T12:00:00")
            LocalDateTime orderedAt, 
            @Schema(description = "주문항목 목록")
            List<OrderItemResponse> items, 
            @Schema(description = "지불금액 (총 주문금액 + 배송비)", example = "26600")
            BigDecimal paymentAmount
    ) {}

    @PostMapping
    @Operation(
            summary = "상품 주문",
            description = "선택한 상품들을 주문합니다. 재고가 부족할 경우 주문이 실패합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (재고 부족, 유효성 검증 실패 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.exception.GlobalExceptionHandler.ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.exception.GlobalExceptionHandler.ErrorResponse.class)
                    )
            )
    })
    public OrderResponse placeOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "주문할 상품 목록",
                    required = true,
                    content = @Content(schema = @Schema(implementation = OrderRequest.class))
            )
            @Valid @RequestBody OrderRequest request) {
        Order order = orderService.placeOrder(
                request.items().stream()
                        .map(i -> new OrderService.OrderItemRequest(i.productNumber(), i.quantity()))
                        .toList()
        );

        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toResponse)
                .toList();

        return new OrderResponse(order.getOrderNumber(), order.getOrderedAt(), items, order.getPaymentAmount());
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getProductNumber(), item.getProductName(), item.getQuantity(), item.getUnitPrice());
    }
}


