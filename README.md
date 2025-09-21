<img src="./assets/logo.png" alt="drawing" width="64"/>

# 29CM 상품 주문 API

Spring Boot 기반으로 구현한 상품 조회 및 주문 처리 API입니다. JPA, Validation, H2 인메모리 데이터베이스를 활용하여 구현되었습니다.

## 📋 프로젝트 개요

### 목적
29CM의 상품 주문 시스템을 모델링한 RESTful API로, 상품 조회와 주문 처리 기능을 제공합니다.

### 주요 기능
- 상품 목록 조회
- 상품 주문 처리 (재고 관리 포함)
- 배송비 정책 적용 (5만원 미만 시 2,500원)
- 멱등성 키(Idempotency-Key) 지원
- 비관적 락을 통한 재고 일관성 보장

## 🏗️ 프로젝트 구조

```
src/main/java/kr/co/_29cm/homework/
├── config/                 # 설정 클래스
│   ├── CsvDataLoader.java         # CSV 데이터 로더
│   ├── OpenApiConfig.java         # Swagger 설정
│   ├── ShippingProperties.java    # 배송비 정책 설정
│   └── ScheduledTasks.java        # 스케줄링 작업
├── controller/             # REST 컨트롤러
│   ├── ProductController.java     # 상품 API
│   └── OrderController.java       # 주문 API
├── domain/                 # 도메인 모델
│   ├── Product.java               # 상품 엔티티
│   ├── Order.java                 # 주문 엔티티
│   ├── OrderItem.java             # 주문 아이템 엔티티
│   ├── IdempotencyKey.java        # 멱등성 키 엔티티
│   └── Money.java                 # 금액 값 객체
├── dto/                    # 데이터 전송 객체
│   ├── request/                   # 요청 DTO
│   └── response/                  # 응답 DTO
├── exception/              # 예외 처리
│   ├── BusinessException.java     # 비즈니스 예외
│   ├── ProductNotFoundException.java
│   ├── InsufficientStockException.java
│   ├── InvalidOrderException.java
│   ├── DuplicateRequestException.java
│   ├── IdempotencyKeyExpiredException.java
│   └── GlobalExceptionHandler.java
├── mapper/                 # 매퍼
│   ├── ProductMapper.java         # 상품 매퍼
│   └── OrderMapper.java           # 주문 매퍼
├── repository/             # 데이터 접근 계층
│   ├── ProductRepository.java     # 상품 리포지토리
│   ├── OrderRepository.java       # 주문 리포지토리
│   └── IdempotencyKeyRepository.java
└── service/                # 비즈니스 로직
    ├── OrderService.java          # 주문 서비스 (비관적 락)
    ├── ShippingCalculator.java    # 배송비 계산기
    └── IdempotencyService.java    # 멱등성 서비스
```

## 🎯 구현 방향

### 1. 도메인 중심 설계 (DDD)
- **값 객체**: `Money` 클래스로 금액 정밀도 관리
- **엔티티**: `Product`, `Order`, `OrderItem`으로 비즈니스 도메인 모델링
- **애그리게이트**: 주문과 주문 아이템 간의 일관성 보장

### 2. 계층형 아키텍처
```
Controller → Service → Repository → Entity
    ↓         ↓         ↓
   DTO    Business   Data
         Logic      Access
```

### 3. 동시성 처리
- **비관적 락 (Pessimistic Locking)**: `@Lock(LockModeType.PESSIMISTIC_WRITE)`으로 재고 일관성 보장
- 주문 처리 시 상품 재고에 대해 배타적 락을 획득하여 동시성 문제 방지

### 4. 예외 처리 및 검증
- **커스텀 예외**: 비즈니스 규칙 위반 시 명확한 예외 정의
- **글로벌 예외 핸들러**: `@RestControllerAdvice`로 일관된 에러 응답
- **입력 검증**: `@Valid` + `jakarta.validation` 어노테이션

### 5. 금액 정밀도 정책
- **정밀도**: `precision=19, scale=2` (소수점 둘째 자리까지)
- **라운딩**: `HALF_UP` 방식으로 일관성 유지
- **값 객체**: `Money` 클래스로 금액 연산 캡슐화

### 6. 멱등성 보장
- **Idempotency-Key**: 클라이언트가 제공하는 고유 키로 중복 요청 방지
- **만료 시간**: 24시간 후 자동 정리
- **상태 관리**: 처리 중/완료/실패 상태 추적

## 🚀 빌드 및 실행 방법

### 필요 조건
- **Java 17** 이상
- **Gradle 8.x** (Gradle Wrapper 포함)

### 1. 빌드 및 실행

#### 방법 1: Gradle Wrapper 사용 (권장)
```bash
# Unix/Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

#### 방법 2: JAR 파일 빌드 후 실행
```bash
# 빌드
./gradlew build

# JAR 파일 실행
java -jar build/libs/29cm-homework-0.0.1-SNAPSHOT.jar
```

### 3. 애플리케이션 확인
- **애플리케이션**: http://localhost:8080
- **H2 콘솔**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:homework`
  - Username: `sa`
  - Password: (비어있음)

### 4. API 문서 확인
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## 🧪 테스트 실행

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 테스트 클래스 실행
```bash
# 상품 도메인 테스트
./gradlew test --tests "*ProductTest*"

# 주문 서비스 테스트
./gradlew test --tests "*OrderServiceTest*"

# 컨트롤러 테스트
./gradlew test --tests "*ControllerTest*"
```

### 테스트 커버리지 확인
```bash
./gradlew test jacocoTestReport
```

## 📊 API 사용 예시

### 상품 목록 조회
```bash
curl -X GET http://localhost:8080/api/products
```

### 주문 생성 (멱등성 키 포함)
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

### 배송비 정책 조회
```bash
curl -X GET http://localhost:8080/api/products/shipping-policy
```

## ⚙️ 설정

### application.yml 주요 설정
```yaml
# 배송비 정책
shipping:
  policy:
    free-shipping-threshold: 50000  # 무료배송 기준 (원)
    fee: 2500                       # 배송비 (원)

# Swagger 설정
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## 🔧 개발 환경 설정

### IDE 설정 (IntelliJ IDEA)
1. **Lombok 플러그인** 설치 및 활성화
2. **Annotation Processing** 활성화
3. **Gradle 프로젝트**로 import

### 데이터베이스 스키마
애플리케이션 시작 시 자동으로 다음 테이블이 생성됩니다:
- `products`: 상품 정보
- `orders`: 주문 정보  
- `order_items`: 주문 아이템 정보
- `idempotency_keys`: 멱등성 키 정보

## 📝 주요 기술 스택
- **Framework**: Spring Boot 3.3.4
- **Database**: H2 (인메모리)
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Jakarta Validation
- **Documentation**: SpringDoc OpenAPI 3
- **Build Tool**: Gradle 8.x
- **Language**: Java 17
- **Testing**: JUnit 5, AssertJ, MockMvc, MoneyTest, IdempotencyServiceTest
