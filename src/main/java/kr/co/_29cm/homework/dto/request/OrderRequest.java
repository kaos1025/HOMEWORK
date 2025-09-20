package kr.co._29cm.homework.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 요청")
public class OrderRequest {

    @Valid
    @NotEmpty(message = "주문 항목은 비어있을 수 없습니다")
    @Size(max = 10, message = "주문 항목은 최대 10개까지 가능합니다")
    @Schema(description = "주문 항목 목록", required = true, maxLength = 10)
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주문 항목")
    public static class OrderItemRequest {

        @NotNull(message = "상품번호는 필수입니다")
        @Schema(description = "상품번호", example = "768848", required = true)
        private Long productNumber;

        @NotNull(message = "주문수량은 필수입니다")
        @Min(value = 1, message = "주문수량은 최소 1개 이상이어야 합니다")
        @Max(value = 999, message = "주문수량은 최대 999개까지 가능합니다")
        @Schema(description = "주문수량", example = "1", minimum = "1", maximum = "999", required = true)
        private Integer quantity;
    }
}
