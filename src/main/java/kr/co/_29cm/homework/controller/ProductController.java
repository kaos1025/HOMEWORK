package kr.co._29cm.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.response.PageResponse;
import kr.co._29cm.homework.dto.response.ProductResponse;
import kr.co._29cm.homework.dto.response.ShippingPolicyResponse;
import kr.co._29cm.homework.mapper.ProductMapper;
import kr.co._29cm.homework.repository.ProductRepository;
import kr.co._29cm.homework.service.ShippingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            summary = "상품 목록 조회 (페이징)",
            description = "등록된 상품의 목록을 페이징으로 조회합니다. 검색어, 정렬, 재고 필터링을 지원합니다."
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
    public kr.co._29cm.homework.dto.response.ApiResponse<PageResponse<ProductResponse>> list(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "정렬 기준 (id, name, price, stockQuantity)", example = "name")
            @RequestParam(defaultValue = "id") String sort,
            
            @Parameter(description = "정렬 방향 (asc, desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String direction,
            
            @Parameter(description = "상품명 검색어", example = "스탠리")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "재고 있는 상품만 조회", example = "false")
            @RequestParam(defaultValue = "false") boolean availableOnly
    ) {
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Product> productPage;
        
        if (availableOnly) {
            productPage = productRepository.findAvailableProducts(pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            productPage = productRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        
        Page<ProductResponse> responsePages = productPage.map(productMapper::toResponse);
        PageResponse<ProductResponse> pageResponse = PageResponse.from(responsePages);
        
        return kr.co._29cm.homework.dto.response.ApiResponse.success(pageResponse, "상품 목록을 성공적으로 조회했습니다");
    }
    
    @GetMapping("/all")
    @Operation(
            summary = "전체 상품 목록 조회 (기존 API)",
            description = "등록된 모든 상품의 목록을 한번에 조회합니다. (하위 호환성을 위해 유지)"
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
    public kr.co._29cm.homework.dto.response.ApiResponse<List<ProductResponse>> listAll() {
        List<Product> products = productRepository.findAll();
        List<ProductResponse> productResponses = productMapper.toResponseList(products);
        return kr.co._29cm.homework.dto.response.ApiResponse.success(productResponses, "상품 목록을 성공적으로 조회했습니다");
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
    public kr.co._29cm.homework.dto.response.ApiResponse<ShippingPolicyResponse> getShippingPolicy() {
        ShippingCalculator.ShippingPolicy policy = shippingCalculator.getShippingPolicy();
        
        ShippingPolicyResponse response = ShippingPolicyResponse.builder()
                .freeShippingThreshold(policy.freeShippingThreshold())
                .shippingFee(policy.shippingFee())
                .isFreeShipping(policy.shippingFee().compareTo(BigDecimal.ZERO) == 0)
                .build();
        
        return kr.co._29cm.homework.dto.response.ApiResponse.success(response, "배송비 정책을 성공적으로 조회했습니다");
    }
}


