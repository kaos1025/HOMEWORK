package kr.co._29cm.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.dto.response.ApiResponse;
import kr.co._29cm.homework.dto.response.OrderResponse;
import kr.co._29cm.homework.mapper.OrderMapper;
import kr.co._29cm.homework.service.IdempotencyService;
import kr.co._29cm.homework.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 관리", description = "상품 주문 API")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(
            summary = "상품 주문",
            description = "선택한 상품들을 주문합니다. 재고가 부족할 경우 주문이 실패합니다. " +
                         "Idempotency-Key 헤더를 통해 중복 요청을 방지할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (재고 부족, 유효성 검증 실패 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복 요청 (Idempotency-Key 충돌)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Idempotency-Key 만료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            )
    })
    public ApiResponse<OrderResponse> placeOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "주문할 상품 목록",
                    required = true,
                    content = @Content(schema = @Schema(implementation = OrderRequest.class))
            )
            @Valid @RequestBody OrderRequest request,
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "멱등성 보장을 위한 고유 키 (선택사항)",
                    example = "req-123456789"
            )
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        List<OrderService.OrderItemRequest> serviceRequests = orderMapper.toServiceRequests(request.getItems());
        
        Order order;
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            // Idempotency-Key가 있는 경우
            order = idempotencyService.processWithIdempotency(idempotencyKey, () -> orderService.placeOrder(serviceRequests));
        } else {
            // 일반 주문 처리
            order = orderService.placeOrder(serviceRequests);
        }
        
        OrderResponse orderResponse = orderMapper.toResponse(order);
        return ApiResponse.success(orderResponse, "주문이 성공적으로 처리되었습니다");
    }
}


