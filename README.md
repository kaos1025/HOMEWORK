<img src="./assets/logo.png" alt="drawing" width="64"/>

# 29CM 상품 주문 API

Spring Boot 기반으로 구현한 상품 조회 및 주문 처리 API입니다. JPA, Validation, H2 인메모리 데이터베이스를 활용하여 구현되었습니다.

## 📋 프로젝트 개요

### 목적
29CM의 상품 주문 시스템을 모델링한 RESTful API로, 상품 조회와 주문 처리 기능을 제공합니다.

### 주요 기능
#### 🛒 상품 관리
- **상품 목록 조회** (페이징, 검색, 정렬, 필터링)
- **재고 있는 상품만 필터링**
- **상품명 검색** (대소문자 구분 없음)
- **다양한 정렬 옵션** (이름, 가격, 재고량)
- **배송비 정책 조회**

#### 📦 주문 처리
- **상품 주문 처리** (재고 관리 포함)
- **주문 상세 조회** (주문번호로 조회)
- **주문 목록 조회** (페이징, 최신순 정렬)
- **기간별 주문 조회**
- **배송비 정책 적용** (5만원 미만 시 2,500원)

#### 🔒 안정성 보장
- **멱등성 키(Idempotency-Key)** 지원
- **비관적 락**을 통한 재고 일관성 보장
- **포괄적인 예외 처리** (9가지 시나리오)
- **입력값 검증** (Jakarta Validation)

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
│   │   └── OrderRequest.java      # 주문 요청 DTO
│   └── response/                  # 응답 DTO
│       ├── ApiResponse.java       # 표준 API 응답
│       ├── OrderResponse.java     # 주문 응답 DTO
│       ├── OrderSummaryResponse.java # 주문 요약 DTO
│       ├── PageResponse.java      # 페이징 응답 DTO
│       ├── ProductResponse.java   # 상품 응답 DTO
│       ├── ShippingPolicyResponse.java # 배송 정책 응답
│       └── ValidationErrorResponse.java # 검증 오류 응답
├── exception/              # 예외 처리
│   ├── BusinessException.java     # 비즈니스 예외
│   ├── ProductNotFoundException.java
│   ├── OrderNotFoundException.java # 주문 조회 예외
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

### 테스트 커버리지 확인 (Jacoco)
```bash
./gradlew test jacocoTestReport
```
커버리지 리포트는 `build/jacocoHtml/index.html`에서 확인할 수 있습니다.

### 📋 테스트 구성

#### 🔧 Service Layer Tests (비즈니스 로직)
```bash
# 주문 서비스 핵심 기능
./gradlew test --tests "*OrderServiceTest*"

# 주문 조회 기능 (페이징, 검색)
./gradlew test --tests "*OrderServiceQueryTest*"

# 예외 처리 시나리오 (9가지)
./gradlew test --tests "*OrderServiceExceptionTest*"

# 멱등성 서비스
./gradlew test --tests "*IdempotencyServiceTest*"

# 배송비 계산기
./gradlew test --tests "*ShippingCalculatorTest*"
```

#### 🌐 Controller Layer Tests (API)
```bash
# 상품 API (페이징, 검색, 정렬, 필터링)
./gradlew test --tests "*ProductControllerTest*"

# 주문 API (생성, 멱등성, 예외처리)
./gradlew test --tests "*OrderControllerTest*"

# 주문 조회 API (상세, 목록, 기간별)
./gradlew test --tests "*OrderQueryControllerTest*"

# 상품 페이징 전용 테스트
./gradlew test --tests "*ProductControllerPaginationTest*"
```

#### 🔗 Integration Tests (통합 테스트)
```bash
# End-to-End 주문 플로우
./gradlew test --tests "*OrderIntegrationTest*"

# 동시성 처리 테스트
./gradlew test --tests "*OrderServiceConcurrencyTest*"
```

#### 🏗️ Domain & Infrastructure Tests
```bash
# 도메인 모델 테스트
./gradlew test --tests "*ProductTest*" --tests "*MoneyTest*"

# 예외 처리 테스트
./gradlew test --tests "*GlobalExceptionHandlerTest*"
```

### 📊 테스트 커버리지 현황

- **총 테스트 수**: 43개 이상
- **테스트 성공률**: 100%
- **커버리지 영역**:
  - ✅ **API Layer**: 페이징, 검색, 주문 조회 등 모든 엔드포인트
  - ✅ **Service Layer**: 비즈니스 로직, 예외 처리, 조회 기능
  - ✅ **Domain Layer**: 엔티티, 값 객체, 비즈니스 규칙
  - ✅ **Exception Handling**: 9가지 예외 시나리오
  - ✅ **Integration**: End-to-End 플로우, 데이터베이스 연동

### 🎯 테스트 전략

#### **Unit Tests** (단위 테스트)
- Mock을 사용한 격리된 테스트
- 빠른 실행 속도
- 특정 기능에 집중

#### **Integration Tests** (통합 테스트)  
- 실제 데이터베이스 사용
- 전체 Spring Context 로드
- End-to-End 시나리오 검증

#### **Controller Tests** (컨트롤러 테스트)
- @WebMvcTest 사용
- HTTP 요청/응답 검증
- API 계약 테스트

## 📊 API 사용 예시

### 상품 관련 API
```bash
# 상품 목록 조회 (페이징)
curl -X GET "http://localhost:8080/api/products?page=0&size=10&sort=name&direction=asc"

# 상품명 검색
curl -X GET "http://localhost:8080/api/products?search=스탠리&availableOnly=true"

# 전체 상품 목록 조회 (기존 API)
curl -X GET http://localhost:8080/api/products/all

# 배송비 정책 조회
curl -X GET http://localhost:8080/api/products/shipping-policy
```

### 주문 관련 API
```bash
# 주문 생성 (멱등성 키 포함)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: req-123456789" \
  -d '{
    "items": [
      {
        "productNumber": 768848,
        "quantity": 1
      }
    ]
  }'

# 주문 상세 조회
curl -X GET http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000

# 주문 목록 조회 (페이징)
curl -X GET "http://localhost:8080/api/orders?page=0&size=10"

# 기간별 주문 조회
curl -X GET "http://localhost:8080/api/orders?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59"
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

### Core Framework
- **Framework**: Spring Boot 3.3.4
- **Database**: H2 (인메모리)
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Jakarta Validation
- **Documentation**: SpringDoc OpenAPI 3
- **Build Tool**: Gradle 8.x
- **Language**: Java 17

### Testing Stack
- **Testing Framework**: JUnit 5
- **Assertion Library**: AssertJ
- **Mock Framework**: Mockito
- **Web Testing**: MockMvc, @WebMvcTest
- **Integration Testing**: @SpringBootTest
- **Code Coverage**: Jacoco
- **Test Categories**:
  - Unit Tests (43개): Service, Controller, Domain 레이어
  - Integration Tests (5개): End-to-End 시나리오
  - Exception Tests (9개): 예외 처리 시나리오
  - Concurrency Tests: 동시성 처리 검증

### API Features
- **Pagination**: Spring Data Pageable 지원
- **Search & Filter**: 상품명 검색, 재고 필터링
- **Sorting**: 다양한 정렬 옵션 (이름, 가격, 재고량)
- **Error Handling**: 표준화된 에러 응답
- **Idempotency**: 중복 요청 방지
- **Concurrency**: 비관적 락을 통한 재고 일관성
