# 29CM 상품 주문 API 명세서

## 개요

29CM 상품 주문 시스템의 RESTful API 명세서입니다. 상품 조회 및 주문 처리 기능을 제공합니다.

- **Base URL**: `http://localhost:8080`
- **API Version**: v1
- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8

## 인증

현재 API는 인증이 필요하지 않습니다.

## 공통 응답 형식

모든 API 응답은 다음과 같은 표준 형식을 따릅니다:

### 성공 응답
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    // 응답 데이터
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": {
      // 추가 에러 정보 (선택사항)
    }
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

## HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | OK - 요청 성공 |
| 400 | Bad Request - 잘못된 요청 |
| 404 | Not Found - 리소스를 찾을 수 없음 |
| 409 | Conflict - 재고 부족 또는 중복 요청 |
| 410 | Gone - Idempotency-Key 만료 |
| 500 | Internal Server Error - 서버 내부 오류 |

## API 엔드포인트

### 1. 상품 목록 조회

#### GET /api/products

전체 상품 목록을 조회합니다.

**요청**
```
GET /api/products
Content-Type: application/json
```

**응답**
```json
{
  "success": true,
  "message": "상품 목록 조회 성공",
  "data": {
    "products": [
      {
        "id": 1,
        "productNumber": 768848,
        "name": "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종",
        "price": 21000,
        "stockQuantity": 45
      },
      {
        "id": 2,
        "productNumber": 759928,
        "name": "마스크 스트랩 분실방지 오염방지 목걸이",
        "price": 2800,
        "stockQuantity": 100
      }
    ]
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

**응답 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 상품 ID |
| `productNumber` | Long | 상품번호 |
| `name` | String | 상품명 |
| `price` | BigDecimal | 판매가격 (원) |
| `stockQuantity` | Integer | 재고수량 |

### 2. 배송비 정책 조회

#### GET /api/products/shipping-policy

현재 배송비 정책 정보를 조회합니다.

**요청**
```
GET /api/products/shipping-policy
Content-Type: application/json
```

**응답**
```json
{
  "success": true,
  "message": "배송비 정책 조회 성공",
  "data": {
    "freeShippingThreshold": 50000,
    "fee": 2500
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

**응답 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `freeShippingThreshold` | BigDecimal | 무료배송 기준 금액 (원) |
| `fee` | BigDecimal | 배송비 (원) |

### 3. 상품 주문

#### POST /api/orders

상품을 주문합니다. Idempotency-Key 헤더를 통해 중복 요청을 방지할 수 있습니다.

**요청 헤더**
| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `Content-Type` | String | O | `application/json` |
| `Idempotency-Key` | String | X | 멱등성 보장을 위한 고유 키 (예: "req-123456789") |

**요청 본문**
```json
{
  "orderItems": [
    {
      "productNumber": 768848,
      "quantity": 1
    },
    {
      "productNumber": 759928,
      "quantity": 2
    }
  ]
}
```

**요청 필드 설명**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderItems` | Array | O | 주문 아이템 목록 |
| `orderItems[].productNumber` | Long | O | 상품번호 (1 이상) |
| `orderItems[].quantity` | Integer | O | 주문 수량 (1 이상, 999 이하) |

**성공 응답 (200 OK)**
```json
{
  "success": true,
  "message": "주문이 성공적으로 처리되었습니다",
  "data": {
    "orderNumber": "550e8400-e29b-41d4-a716-446655440000",
    "orderedAt": "2025-01-19T12:00:00",
    "orderItems": [
      {
        "productNumber": 768848,
        "productName": "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종",
        "price": 21000,
        "quantity": 1,
        "amount": 21000
      },
      {
        "productNumber": 759928,
        "productName": "마스크 스트랩 분실방지 오염방지 목걸이",
        "price": 2800,
        "quantity": 2,
        "amount": 5600
      }
    ],
    "totalAmount": 26600,
    "shippingFee": 2500,
    "totalPayment": 29100
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

**응답 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `orderNumber` | String | 주문번호 (UUID) |
| `orderedAt` | String | 주문 일시 (ISO 8601) |
| `orderItems` | Array | 주문 아이템 목록 |
| `orderItems[].productNumber` | Long | 상품번호 |
| `orderItems[].productName` | String | 상품명 |
| `orderItems[].price` | BigDecimal | 단가 |
| `orderItems[].quantity` | Integer | 주문 수량 |
| `orderItems[].amount` | BigDecimal | 소계 (단가 × 수량) |
| `totalAmount` | BigDecimal | 주문 총액 |
| `shippingFee` | BigDecimal | 배송비 |
| `totalPayment` | BigDecimal | 총 결제금액 |

## 에러 응답

### 400 Bad Request - 유효성 검증 실패

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값 검증에 실패했습니다",
    "data": {
      "fieldErrors": {
        "orderItems[0].quantity": [
          "수량은 1 이상이어야 합니다"
        ]
      },
      "globalErrors": []
    }
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 404 Not Found - 상품을 찾을 수 없음

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품번호 999999인 상품을 찾을 수 없습니다"
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 409 Conflict - 재고 부족

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "상품번호 768848, 상품명 '[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종'의 재고가 부족합니다. 요청 수량: 50, 현재 재고: 45"
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 409 Conflict - 중복 요청

```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_REQUEST",
    "message": "동일한 Idempotency-Key로 이미 처리된 요청입니다"
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 410 Gone - Idempotency-Key 만료

```json
{
  "success": false,
  "error": {
    "code": "IDEMPOTENCY_KEY_EXPIRED",
    "message": "Idempotency-Key가 만료되었습니다. 새로운 키로 다시 요청해주세요"
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

### 500 Internal Server Error

```json
{
  "success": false,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "서버 내부 오류가 발생했습니다."
  },
  "timestamp": "2025-01-19T12:00:00"
}
```

## 비즈니스 규칙

### 배송비 정책
- 주문 총액이 **50,000원 미만**인 경우: 배송비 **2,500원** 부과
- 주문 총액이 **50,000원 이상**인 경우: **무료배송** (배송비 0원)

### 재고 관리
- 주문 시 해당 상품의 재고가 자동으로 차감됩니다
- 재고 부족 시 주문이 실패합니다
- 동시성 처리를 통해 재고 일관성을 보장합니다

### 멱등성 (Idempotency)
- `Idempotency-Key` 헤더를 제공하면 동일한 요청의 중복 처리를 방지합니다
- 키는 24시간 후 자동으로 만료됩니다
- 동일한 키로 재요청 시 이전 결과를 반환합니다

### 금액 정밀도
- 모든 금액은 소수점 둘째 자리까지 지원합니다
- 금액 계산 시 HALF_UP 라운딩을 적용합니다

## 동시성 처리

시스템은 비관적 락을 사용하여 재고 일관성을 보장합니다:

- **비관적 락 (Pessimistic Locking)**: 주문 처리 시 상품 재고에 대해 배타적 락을 획득하여 동시성 문제를 방지합니다.

## 사용 예시

### cURL 예시

#### 상품 목록 조회
```bash
curl -X GET http://localhost:8080/api/products \
  -H "Content-Type: application/json"
```

#### 배송비 정책 조회
```bash
curl -X GET http://localhost:8080/api/products/shipping-policy \
  -H "Content-Type: application/json"
```

#### 주문 생성 (멱등성 키 포함)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: req-123456789" \
  -d '{
    "orderItems": [
      {
        "productNumber": 768848,
        "quantity": 1
      }
    ]
  }'
```

#### 주문 생성 (멱등성 키 없음)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderItems": [
      {
        "productNumber": 768848,
        "quantity": 1
      },
      {
        "productNumber": 759928,
        "quantity": 2
      }
    ]
  }'
```
## 제한사항

- 최대 주문 수량: 999개
- 최소 주문 수량: 1개
- 한 번에 주문할 수 있는 상품 종류: 제한 없음
- Idempotency-Key 만료 시간: 24시간
- 금액 정밀도: 소수점 둘째 자리까지


