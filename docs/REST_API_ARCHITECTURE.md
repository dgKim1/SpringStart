# RESTful API 설계 원칙

## 자원(Resource)과 URI

- **핵심**: URI는 자원(Resource)을 **명사**로 표현합니다.
- 자원은 DB 데이터뿐만 아니라 이미지, 문서 등 모든 것이 될 수 있습니다.

**URI 설계 예시**

```
GET /users                    # 모든 사용자
GET /users/123                # ID가 123인 사용자
GET /users/123/purchases      # 123번 사용자의 모든 구매 내역
```

---

## 행위(Verb)와 HTTP 메서드

자원에 대한 행위(CRUD)는 **HTTP 메서드**로 표현합니다.

| HTTP 메서드 | 주요 역할 | 멱등성(Idempotent) | 안전성(Safe) |
|------------|---------|-----------------|------------|
| **GET** | 자원 **조회** | O | O |
| **POST** | 자원 **생성** | X | X |
| **PUT** | 자원 **전체 교체(수정)** | O | X |
| **PATCH** | 자원 **부분 수정** | X | X |
| **DELETE** | 자원 **삭제** | O | X |

> **멱등성(Idempotent)**: 여러 번 수행해도 결과가 같은 성질. DELETE는 이미 삭제된 것을 또 삭제해도 '삭제된 상태'는 변하지 않으므로 멱등합니다.
>
> **안전성(Safe)**: 연산을 수행해도 서버의 상태가 변하지 않는 성질. GET이 유일합니다.

---

## 무상태성 (Stateless)

- 서버는 클라이언트의 이전 상태를 저장하거나 기억하지 않습니다. 모든 요청은 그 자체로 완결되어야 합니다.
- 서버가 세션 상태를 관리할 필요가 없으므로 어떤 서버 인스턴스가 요청을 처리하든 상관없어집니다.
- 이는 **시스템의 확장성(Scalability)을 확보**하는 데 매우 중요합니다.

---

## 엔드포인트 설계 Best Practice

### 1. 명확하고 일관된 URI 설계

- **URI는 소문자 + 하이픈(`-`)으로 구분합니다.**
  - (Good) `/order-items`
  - (Bad) `/order_items`, `/orderItems`

- **컬렉션(목록)은 복수형 명사를 사용합니다.**
  - (Good) `/users`, `/products`
  - (Bad) `/user`, `/product`

- **자원 간 계층 관계를 URI 경로로 표현합니다.**
  ```
  /products/{id}/reviews
  /users/{userId}/purchases/{purchaseId}
  ```

### 2. 필터링, 정렬, 페이징

특정 조건에 맞는 데이터 조회 시 **Query Parameter**를 활용합니다.

```
GET /products?category=electronics&page=2&size=10&sort=price,desc
```

| 파라미터 | 역할 |
|---------|------|
| `category=electronics` | 필터링 |
| `page=2&size=10` | 페이징 |
| `sort=price,desc` | 정렬 (가격 내림차순) |

### 3. 행동(Action) 표현하기

CRUD로 표현하기 어려운 동작(환불, 승인 등)은 **행동을 자원의 하위 리소스로 취급**하여 `POST`로 처리합니다.

```
POST /purchases/{purchaseId}/refund   # 해당 구매에 대한 환불 요청
POST /users/{userId}/approve          # 해당 사용자 승인
```

---

## URI는 '자원'을, 행위는 'HTTP 메서드'로 표현하라

가장 중요한 원칙입니다. URI 주소는 **행위(Action)가 아닌 자원(Resource)의 이름**을 나타내야 합니다.

**잘못된 예 (X)**: URI에 행위가 포함됨
```
/getProducts
/createProduct
/deleteProductById/1
```

**올바른 예 (O)**: URI는 자원을, HTTP 메서드는 행위를 표현
```
GET    /api/products      # 모든 상품 조회
GET    /api/products/1    # 1번 상품 조회
POST   /api/products      # 새로운 상품 생성
PUT    /api/products/1    # 1번 상품 수정
DELETE /api/products/1    # 1번 상품 삭제
```

---

## 응답은 'HTTP 상태 코드'로 말하라

클라이언트에게 작업 결과를 명확하게 전달하기 위해 HTTP 상태 코드를 적극적으로 활용합니다. 모든 응답을 `200 OK`로만 처리하는 것은 좋은 방법이 아닙니다.

| 상태 코드 | 의미 | 주로 사용하는 메서드 |
|---------|------|------------------|
| `200 OK` | 요청이 성공적으로 처리됨 | `GET`, `PUT` |
| `201 Created` | 리소스가 성공적으로 생성됨 | `POST` |
| `204 No Content` | 성공했지만 반환할 데이터 없음 | `DELETE` |
| `400 Bad Request` | 클라이언트 요청이 잘못됨 (유효성 검증 실패 등) | - |
| `404 Not Found` | 요청한 리소스가 존재하지 않음 | - |
| `500 Internal Server Error` | 서버 내부 오류 | - |

> `201 Created` 응답 시 헤더의 `Location`에 생성된 리소스 URI를 포함해주는 것이 좋습니다.

---

## 요청과 응답에는 DTO를 사용하라 (Entity 노출 금지)

Entity를 API의 요청/응답 객체로 직접 사용하면 DB 스키마가 그대로 노출되고 유지보수가 어려워집니다. 반드시 **역할을 분리**해야 합니다.

- **`Request DTO`**: 클라이언트 요청 데이터를 담는 객체. `@NotNull`, `@Size` 등 **데이터 유효성 검증**의 책임을 가집니다.
- **`Response DTO`**: 클라이언트에게 반환할 데이터를 담는 객체. Entity에서 필요한 데이터만 선별하여 **화면에 보여줄 정보**를 구성합니다.

---

## Controller 예제 (DTO + 상태 코드 적용)

### Request / Response DTO

```java
// ProductRequest.java
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {

    Long categoryId;

    @NotNull
    String name;

    String description;

    @NotNull
    @Positive
    BigDecimal price;

    @NotNull
    @PositiveOrZero
    Integer stock;
}

// ProductResponse.java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {

    Long id;
    Long categoryId;
    String name;
    String description;
    BigDecimal price;
    Integer stock;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;
}
```

### ProductController

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.deleteById(id);
    }
}
```

### ProductService

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts() {
        return List.of();
    }

    public ProductResponse getProductById(Long id) {
        return ProductResponse.builder().build();
    }

    public ProductResponse create(ProductRequest request) {
        return ProductResponse.builder().build();
    }

    public ProductResponse update(Long id, ProductRequest request) {
        return ProductResponse.builder().build();
    }

    public void deleteById(Long id) {
    }
}
```
