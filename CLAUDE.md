# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # 전체 빌드
./gradlew clean build    # 클린 빌드
./gradlew bootRun        # 애플리케이션 실행
./gradlew test           # 전체 테스트 실행
./gradlew test --tests "com.sparta.msa.lesson.SomeTest"  # 단일 테스트 실행
```

## Requirements

- Java 21
- PostgreSQL (localhost:5432, DB: `sparta`, user: `postgres`, password: `postgres`)

## Architecture

### Package Structure

```
com.sparta.msa.lesson
├── global/
│   └── config/        # SwaggerConfig 등 전역 설정
└── LessonApplication  # 진입점
```

현재 골격(skeleton) 상태의 프로젝트다. 실제 도메인 구현(Entity, Repository, Service, Controller)은 아직 없으며, 레이어별 패키지를 `global/` 아래 또는 도메인별 패키지로 추가해 나간다.

### Key Technology Decisions

- **Servlet Container:** Tomcat 대신 Undertow 사용 (`build.gradle`에서 Tomcat 제외 처리됨)
- **Schema 관리:** JPA `ddl-auto=none`, Flyway로 마이그레이션 관리 (`src/main/resources/db/migration/`)
- **QueryDSL:** 타입 안전 쿼리를 위해 APT(annotation processor) 설정됨 — `QClass`는 빌드 시 생성됨
- **객체 매핑:** MapStruct 사용 (Lombok과 함께 annotation processor 순서 주의)
- **서비스 간 통신:** Spring Cloud OpenFeign 포함

### Security & API Docs

- Spring Security 기본 인증: username=`user`, password=`1234`
- JWT Bearer 인증 스킴이 Swagger에 등록되어 있음
- Swagger UI: http://localhost:8080/swagger-ui.html

### Database Migrations

`src/main/resources/db/migration/` 아래 Flyway 네이밍 규칙(`V{숫자}__{설명}.sql`)으로 추가한다. `baseline-on-migrate=true`가 설정되어 있어 기존 DB에서도 초기 마이그레이션이 동작한다.

## 아키텍처 설계 규칙

### 패키지 구조

```
com.sparta.msa.lesson
├── domain/                  # 도메인별 패키지
│   └── member/
│       ├── controller/      # HTTP 요청 처리 (API 엔드포인트)
│       ├── service/         # 비즈니스 로직
│       ├── dto/             # Request/Response DTO
│       ├── entity/          # JPA 엔티티
│       └── repository/      # JPA 인터페이스
├── common/                  # 여러 도메인에서 공통으로 사용하는 코드
│   └── util/                # 유틸리티 클래스 (날짜, 암호화 등)
└── global/                  # 애플리케이션 전역 설정
    └── config/              # SecurityConfig 등 설정 클래스
```

- **domain:** 도메인 단위로 패키지를 분리하여 관련 코드를 응집시킨다. 새 기능은 `domain/{도메인명}/` 아래에 controller·service·dto·entity·repository 패키지를 만들어 추가한다.
- **common:** 특정 도메인에 종속되지 않는 유틸리티성 코드를 둔다.
- **global:** 보안, CORS, 로깅 등 애플리케이션 전반에 적용되는 설정 코드를 둔다.

## JPA Entity 설계 규칙

### 기본 구조

```java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "테이블명")
public class SomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50)
    String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    LocalDateTime updatedAt;

    @Builder
    public SomeEntity(String name) {
        this.name = name;
    }
}
```

### 핵심 규칙

- **기본 생성자:** `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA 필수, 외부 직접 생성 방지
- **생성자/빌더:** `@Builder`는 id·타임스탬프를 제외한 비즈니스 필드만 파라미터로 받음
- **`@DynamicInsert` / `@DynamicUpdate`:** DB DEFAULT 값 활용 및 변경 필드만 UPDATE하기 위해 항상 적용
- **타임스탬프:** `@CreationTimestamp`(createdAt, `updatable = false`) + `@UpdateTimestamp`(updatedAt)
- **네이밍:** Java camelCase → DB snake_case 변환은 SpringPhysicalNamingStrategy가 자동 처리하므로 `@Column(name=...)` 생략 가능
- **예약어 회피:** `users`, `orders` 등 복수형으로 테이블명 지정하여 DB 예약어(`USER`, `ORDER`) 충돌 방지

## 데이터베이스 설계 규칙

### PK 정의

- **대리 키(Surrogate Key) 사용**: `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` — 비즈니스와 무관하게 항상 고유함이 보장되는 PK
- 이메일 등 자연 키(Natural Key)를 PK로 쓰지 않는다 (변경 가능성, 개인정보 이슈)

### 연관관계 매핑 규칙

- **연관관계의 주인(Owner)**: 외래 키(FK)가 있는 **N쪽(`@ManyToOne`)** 이 항상 주인 — `@JoinColumn`으로 FK 컬럼을 명시
- **주인이 아닌 쪽**: `@OneToMany(mappedBy = "필드명")` 으로 읽기 전용임을 명시
- **페치 전략**: 연관관계는 항상 `fetch = FetchType.LAZY` (지연 로딩) 기본 적용
- **`@ManyToMany` 금지**: N:N 관계는 연결 테이블을 별도 엔티티로 승격하여 두 개의 1:N 관계로 풀어낸다
  - 연결 엔티티에는 관계 자체의 의미 있는 데이터(수량, 주문 시점 가격 등)를 저장할 수 있다
- **양방향 관계 주의사항**:
  - 연관관계 편의 메서드로 양쪽 상태를 한 번에 동기화한다
  - `toString()` · JSON 직렬화 시 무한 순환 참조 방지 (`@ToString.Exclude` 또는 `@JsonManagedReference`/`@JsonBackReference`)

### FOREIGN KEY 제약조건 정책

- **MSA / 대규모 트래픽**: DB 레벨 FK 제약조건을 사용하지 않고 **애플리케이션 레벨(ORM · 비즈니스 로직)에서 데이터 정합성 관리**
- 모노리스에서는 FK 사용을 고려할 수 있으나, 이 프로젝트는 MSA 방향이므로 FK 제약조건을 걸지 않는다

### 정규화 전략

- **기본은 제3정규형(3NF)**: 데이터 중복 최소화, 갱신·삽입·삭제 이상 현상 방지
- **반정규화는 성능 병목이 측정·증명된 경우에만 제한적으로 적용**
  - 예: 빈번하게 필요한 집계값(`like_count`, `comment_count`)을 컬럼으로 미리 저장
- 원칙: *"일단 정규화하고, 성능 문제가 발생하면 측정하여 필요한 곳만 반정규화하라"*

### 주요 테이블 관계 예시 (쇼핑몰)

```
users --<1:N>-- orders --<1:N>-- order_products --<N:1>-- products
                                                               |
                                                             <N:1>
                                                           categories (자기 참조)
```

- `users : orders` = 1:N
- `orders : order_products` = 1:N
- `products : order_products` = 1:N
- `categories : products` = 1:N
- `categories : categories` = 1:N (자기 참조, `parent_id`)

## RESTful API 설계 규칙

### URI 설계

- **URI는 자원(명사)으로, 행위는 HTTP 메서드로 표현한다.**
  - (Bad) `/getProducts`, `/createProduct`, `/deleteProductById/1`
  - (Good) `GET /api/products`, `POST /api/products`, `DELETE /api/products/1`
- **URI는 소문자 + 하이픈(`-`)으로 구분한다.** 언더스코어(`_`)나 camelCase 금지
- **컬렉션은 복수형 명사를 사용한다.** `/users`, `/products`
- **자원 간 계층은 URI 경로로 표현한다.** `/users/{userId}/purchases/{purchaseId}`
- **필터링·정렬·페이징은 Query Parameter로 표현한다.**
  - `GET /products?category=electronics&page=2&size=10&sort=price,desc`
- **CRUD로 표현하기 어려운 행동은 하위 리소스 + `POST`로 처리한다.**
  - `POST /orders/{orderId}/refund`, `POST /users/{userId}/approve`

### HTTP 상태 코드

| 상태 코드 | 사용 시점 |
|---------|---------|
| `200 OK` | `GET`, `PUT` 성공 |
| `201 Created` | `POST` 로 리소스 생성 성공 |
| `204 No Content` | `DELETE` 성공 (반환 데이터 없음) |
| `400 Bad Request` | 유효성 검증 실패 등 클라이언트 오류 |
| `404 Not Found` | 요청한 리소스가 존재하지 않음 |
| `500 Internal Server Error` | 서버 내부 오류 |

### DTO 사용 원칙

- **Entity를 API 요청/응답에 직접 사용하지 않는다.**
- **`Request DTO`**: 유효성 검증(`@NotNull`, `@Positive` 등) 책임
- **`Response DTO`**: 필요한 데이터만 선별하여 반환, `@Builder` 적용

## 공통 응답 및 예외 처리 규칙

### 공통 응답 객체

- **모든 API 응답은 `ApiResponse<T>`로 감싸서 반환한다.**
  - 성공 시: `ApiResponse.ok(data)` 또는 `ApiResponse.ok()`
  - 실패 시: `ApiResponse.fail(httpStatus, errorCode, errorMessage)`
- `@JsonInclude(NON_NULL)`을 적용하여 null 필드는 JSON에서 제외한다.

### 예외 처리 규칙

- **비즈니스 예외는 `DomainException`으로 던진다.** 서비스 코드에서 `try-catch`로 직접 처리하지 않는다.
- **에러 코드와 메시지는 `DomainExceptionCode` Enum에서 중앙 관리한다.**
- **`@RestControllerAdvice`의 `GlobalExceptionHandler`가 모든 예외를 중앙 처리한다.**

| 예외 타입 | 상태 코드 |
|---------|---------|
| `DomainException` | Enum에 정의된 코드 |
| `MethodArgumentNotValidException` / `BindException` | `400` |
| `Exception` (그 외) | `500` |

```java
// 서비스에서 예외 사용 예시
Product product = productRepository.findById(id)
    .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_PRODUCT));
```

## 의존성 주입 규칙

- **생성자 주입을 항상 사용한다**: `@Autowired` 필드 주입, setter 주입은 사용하지 않는다.
- **`@RequiredArgsConstructor` + `final` 조합을 기본으로 사용한다**: Lombok이 생성자를 자동 생성하므로 코드가 간결해진다.
- **이유**: `final`로 불변성 보장, 순환 참조를 애플리케이션 시작 시점에 감지, 테스트 시 순수 Java로 의존성 주입 가능

```java
// 올바른 방식
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
}
```

## 서비스 레이어 데이터 흐름

### 레이어별 역할

- **Controller**: `Request` / `Response` 객체만 다루고, Entity를 직접 반환하지 않는다.
- **Service**: 비즈니스 로직의 중심. `Dto`를 받아 `Entity`를 생성·조작하고 `Response`로 변환하여 반환한다.
- **Repository**: `Entity`만 다룬다. DB 접근 역할만 수행한다.
- **Entity를 외부에 노출하지 않는다**: API 응답에 Entity를 직접 사용하면 DB 구조가 외부에 노출되고 순환 참조 문제가 발생할 수 있다.

### 데이터 변환 순서

```
Client
  │  JSON 요청
  ▼
Controller        UserCreateRequest  (유효성 검증)
  │  변환
  ▼
Service           UserCreateDto      (비즈니스 로직 처리)
  │  Entity 생성
  ▼
Repository        User Entity        (DB 저장)
  │  반환
  ▼
Service           User Entity
  │  변환
  ▼
Controller        UserResponse       (응답에 필요한 데이터만)
  │  JSON 응답
  ▼
Client
```
