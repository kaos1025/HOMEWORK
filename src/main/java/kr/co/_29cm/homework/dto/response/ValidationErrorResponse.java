package kr.co._29cm.homework.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "유효성 검증 에러 응답")
public class ValidationErrorResponse {

    @Schema(description = "필드별 에러 정보")
    private Map<String, List<String>> fieldErrors;

    @Schema(description = "전역 에러 메시지")
    private List<String> globalErrors;

    @Schema(description = "에러 총 개수")
    private Integer errorCount;

    public static ValidationErrorResponse from(Map<String, List<String>> fieldErrors, List<String> globalErrors) {
        int totalErrors = fieldErrors.values().stream()
                .mapToInt(List::size)
                .sum() + globalErrors.size();

        return ValidationErrorResponse.builder()
                .fieldErrors(fieldErrors)
                .globalErrors(globalErrors)
                .errorCount(totalErrors)
                .build();
    }
}
