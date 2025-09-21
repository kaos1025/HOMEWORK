package kr.co._29cm.homework.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 * 
 * Spring Data의 Page 객체를 클라이언트 친화적인 형태로 변환하여 제공합니다.
 * 
 * @param <T> 페이징 대상 데이터 타입
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Getter
@AllArgsConstructor
@Schema(description = "페이징 응답")
public class PageResponse<T> {
    
    @Schema(description = "현재 페이지 데이터")
    private List<T> content;
    
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int page;
    
    @Schema(description = "페이지 크기", example = "10")
    private int size;
    
    @Schema(description = "전체 요소 수", example = "100")
    private long totalElements;
    
    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;
    
    @Schema(description = "첫 번째 페이지 여부", example = "true")
    private boolean first;
    
    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;
    
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
    
    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;
    
    /**
     * Spring Data Page 객체로부터 PageResponse 생성
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
