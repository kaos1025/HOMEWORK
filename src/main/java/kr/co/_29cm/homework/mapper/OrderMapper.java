package kr.co._29cm.homework.mapper;

import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.dto.response.OrderResponse;
import kr.co._29cm.homework.service.OrderService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public List<OrderService.OrderItemRequest> toServiceRequests(List<OrderRequest.OrderItemRequest> requests) {
        return requests.stream()
                .map(req -> new OrderService.OrderItemRequest(req.getProductNumber(), req.getQuantity()))
                .toList();
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderedAt(order.getOrderedAt())
                .items(itemResponses)
                .paymentAmount(order.getPaymentAmount())
                .build();
    }

    private OrderResponse.OrderItemResponse toItemResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
                .productNumber(item.getProductNumber())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getTotalPrice())
                .build();
    }
}
