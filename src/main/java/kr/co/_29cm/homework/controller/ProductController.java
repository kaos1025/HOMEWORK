package kr.co._29cm.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co._29cm.homework.dto.response.ApiResponse;
import kr.co._29cm.homework.dto.response.ProductResponse;
import kr.co._29cm.homework.dto.response.ShippingPolicyResponse;
import kr.co._29cm.homework.mapper.ProductMapper;
import kr.co._29cm.homework.repository.ProductRepository;
import kr.co._29cm.homework.service.ShippingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품 관리", description = "상품 조회 API")
public class ProductController {

    private final ProductRepository productRepository;
    private final ShippingCalculator shippingCalculator;
    private final ProductMapper productMapper;

    @GetMapping
    @Operation(
            summary = "상품 목록 조회",
            description = "등록된 모든 상품의 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            )
    })
    public ApiResponse<List<ProductResponse>> list() {
        List<Product> products = productRepository.findAll();
        List<ProductResponse> productResponses = productMapper.toResponseList(products);
        return ApiResponse.success(productResponses, "상품 목록을 성공적으로 조회했습니다");
    }

    @GetMapping("/shipping-policy")
    @Operation(
            summary = "배송비 정책 조회",
            description = "현재 적용된 배송비 정책 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송비 정책 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = kr.co._29cm.homework.dto.response.ApiResponse.class)
                    )
            )
    })
    public ApiResponse<ShippingPolicyResponse> getShippingPolicy() {
        ShippingCalculator.ShippingPolicy policy = shippingCalculator.getShippingPolicy();
        
        ShippingPolicyResponse response = ShippingPolicyResponse.builder()
                .freeShippingThreshold(policy.freeShippingThreshold())
                .shippingFee(policy.shippingFee())
                .isFreeShipping(policy.shippingFee().compareTo(BigDecimal.ZERO) == 0)
                .build();
        
        return ApiResponse.success(response, "배송비 정책을 성공적으로 조회했습니다");
    }
}


