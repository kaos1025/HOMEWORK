<img src="./assets/logo.png" alt="drawing" width="64"/>

# 29CM 상품 주문 API

Spring Boot(JPA, Validation) + H2 인메모리 DB로 구현한 상품 조회/주문 API입니다.

## 실행 방법

1) 빌드 및 실행

```bash
./gradlew bootRun
```

Windows:

```bash
gradlew.bat bootRun
```

2) H2 콘솔: `/h2-console` (JDBC URL: `jdbc:h2:mem:homework`, user `sa`)

## 주요 설계

- 도메인: `Product`, `Order`, `OrderItem`
- 초기 데이터 적재: `products.csv`를 `CsvDataLoader`가 애플리케이션 시작 시 읽어 `products` 테이블에 삽입
- 주문 처리: 트랜잭션 + 비관적 락으로 재고 일관성 보장, 5만원 미만 배송비 2,500원
- Open-in-view 비활성화, 컨트롤러 레벨 DTO로 응답

## API 문서

### Swagger UI
애플리케이션 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### API 엔드포인트

- **GET `/api/products`** - 상품 목록 조회
  - 응답: `[ { id, productNumber, name, price, stockQuantity } ]`

- **POST `/api/orders`** - 상품 주문
  - 요청 Body:
    ```json
    {
      "items": [
        { "productNumber": 768848, "quantity": 1 },
        { "productNumber": 759928, "quantity": 2 }
      ]
    }
    ```
  - 응답 Body:
    ```json
    {
      "orderNumber": "550e8400-e29b-41d4-a716-446655440000",
      "orderedAt": "2025-01-19T12:00:00",
      "items": [
        { "productNumber": 768848, "productName": "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", "quantity": 1, "unitPrice": 21000 },
        { "productNumber": 759928, "productName": "마스크 스트랩 분실방지 오염방지 목걸이", "quantity": 2, "unitPrice": 2800 }
      ],
      "paymentAmount": 26600
    }
    ```

## 테스트

```bash
./gradlew test
```

## 참고

- 과제 요구사항: [HOMEWORK.md](HOMEWORK.md)