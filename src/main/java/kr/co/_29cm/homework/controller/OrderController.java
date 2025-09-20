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
            @Valid @RequestBody OrderRequest request) {
        
        List<OrderService.OrderItemRequest> serviceRequests = orderMapper.toServiceRequests(request.getItems());
        Order order = orderService.placeOrder(serviceRequests);
        OrderResponse orderResponse = orderMapper.toResponse(order);
        
        return ApiResponse.success(orderResponse, "주문이 성공적으로 처리되었습니다");
    }
}


