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

## API 명세

- GET `/api/products`
  - 응답: `[ { id, productNumber, name, price, stockQuantity } ]`

- POST `/api/orders`
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
      "orderNumber": "...",
      "orderedAt": "2025-09-19T12:00:00",
      "items": [
        { "productNumber": 768848, "productName": "...", "quantity": 1, "unitPrice": 21000 },
        { "productNumber": 759928, "productName": "...", "quantity": 2, "unitPrice": 2800 }
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