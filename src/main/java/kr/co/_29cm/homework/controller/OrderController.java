package kr.co._29cm.homework.controller;

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
public class OrderController {

    private final OrderService orderService;

    public record OrderRequestItem(@NotNull Long productNumber, @NotNull @Min(1) Integer quantity) {}
    public record OrderRequest(@Valid List<OrderRequestItem> items) {}

    public record OrderItemResponse(Long productNumber, String productName, Integer quantity, BigDecimal unitPrice) {}
    public record OrderResponse(String orderNumber, LocalDateTime orderedAt, List<OrderItemResponse> items, BigDecimal paymentAmount) {}

    @PostMapping
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request) {
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


