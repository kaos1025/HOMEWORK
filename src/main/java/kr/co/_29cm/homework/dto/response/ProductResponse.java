package kr.co._29cm.homework.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 응답")
public class ProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Schema(description = "상품번호", example = "768848")
    private Long productNumber;

    @Schema(description = "상품명", example = "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
    private String name;

    @Schema(description = "판매가격", example = "21000")
    private BigDecimal price;

    @Schema(description = "재고수량", example = "45")
    private Integer stockQuantity;
}
