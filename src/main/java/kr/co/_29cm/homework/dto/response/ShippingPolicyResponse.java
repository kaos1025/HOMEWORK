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
@Schema(description = "배송비 정책 응답")
public class ShippingPolicyResponse {

    @Schema(description = "무료배송 기준 금액", example = "50000")
    private BigDecimal freeShippingThreshold;

    @Schema(description = "배송비", example = "2500")
    private BigDecimal shippingFee;

    @Schema(description = "무료배송 여부", example = "true")
    private Boolean isFreeShipping;
}
