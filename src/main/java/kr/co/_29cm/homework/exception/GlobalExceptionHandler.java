package kr.co._29cm.homework.exception;

import kr.co._29cm.homework.dto.response.ApiResponse;
import kr.co._29cm.homework.dto.response.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        HttpStatus status = determineHttpStatus(e);
        
        ApiResponse<Object> response = ApiResponse.error(
                e.getMessage(),
                e.getCode()
        );
        
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, List<String>> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
        
        List<String> globalErrors = e.getBindingResult().getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ValidationErrorResponse validationError = ValidationErrorResponse.from(fieldErrors, globalErrors);
        
        ApiResponse<ValidationErrorResponse> response = ApiResponse.error(
                "입력값 검증에 실패했습니다",
                "VALIDATION_ERROR",
                validationError
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleBindException(BindException e) {
        Map<String, List<String>> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
        
        List<String> globalErrors = e.getBindingResult().getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ValidationErrorResponse validationError = ValidationErrorResponse.from(fieldErrors, globalErrors);
        
        ApiResponse<ValidationErrorResponse> response = ApiResponse.error(
                "데이터 바인딩에 실패했습니다",
                "BIND_ERROR",
                validationError
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception e) {
        ApiResponse<Object> response = ApiResponse.error(
                "서버 내부 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus determineHttpStatus(BusinessException e) {
        return switch (e.getCode()) {
            case "PRODUCT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INSUFFICIENT_STOCK", "INVALID_ORDER" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
