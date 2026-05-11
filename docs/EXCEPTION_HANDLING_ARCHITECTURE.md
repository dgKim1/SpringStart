# 공통 응답 처리와 예외 핸들링

## 1. 공통 응답 객체 설계 (ApiResponse)

### 필요성

1. **예측 가능성**: 모든 API 응답이 동일한 구조를 가지므로 클라이언트가 결과를 예측하고 처리하기 쉬워집니다.
2. **유연한 확장**: 공통 포맷 안에 페이징 정보 등 추가 메타데이터를 담기 용이합니다.

### ApiResponse\<T\> 설계

성공 시 `data` 필드에, 실패 시 `error` 필드에 정보를 담는 제네릭 클래스입니다.

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    Error error;
    T data;

    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder().build();
    }

    public static <T> ApiResponse<T> ok(T message) {
        return ApiResponse.<T>builder()
            .data(message)
            .build();
    }

    public static <T> ResponseEntity<ApiResponse<T>> fail(HttpStatus httpStatus, String errorCode,
        String errorMessage) {
        return ResponseEntity.status(httpStatus)
            .body(ApiResponse.<T>builder()
                .error(Error.of(errorCode, errorMessage))
                .build());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Error(String errorCode, String errorMessage) {

        public static Error of(String errorCode, String errorMessage) {
            return new Error(errorCode, errorMessage);
        }
    }
}
```

### Controller 적용 예시

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        return ApiResponse.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return ApiResponse.ok();
    }
}
```

---

## 2. 전역 예외 핸들링

애플리케이션의 모든 예외를 한 곳에서 처리하여, 일관되고 정제된 오류 메시지를 클라이언트에게 반환합니다.

### 1단계: 커스텀 예외와 에러 코드 정의

**DomainExceptionCode** — 오류 코드와 메시지를 한 곳에서 관리합니다.

```java
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum DomainExceptionCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 누락되었습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다."),
    JSON_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Json 데이터 처리 중 에러가 발생하였습니다."),
    ;

    final HttpStatus status;
    final String message;
}
```

**DomainException** — 비즈니스 로직에서 발생하는 예외를 위한 커스텀 클래스입니다.

```java
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DomainException extends RuntimeException {

    HttpStatus httpStatus;
    String code;

    public DomainException(DomainExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.httpStatus = exceptionCode.getStatus();
        this.code = exceptionCode.name();
    }
}
```

### 2단계: GlobalExceptionHandler 구현

`@RestControllerAdvice`로 모든 Controller에서 발생하는 예외를 중앙에서 처리합니다.

```java
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String SERVER_ERROR = "SERVER_ERROR";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        log.warn("[DomainException] : code={}, message={}", ex.getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getHttpStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex) {
        String errorMessage = extractErrorMessages(ex);
        log.warn("[ValidationException] : {}", errorMessage);
        return ApiResponse.fail(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, errorMessage);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String errorMessage = extractErrorMessages(ex);
        log.warn("[BindException] : {}", errorMessage);
        return ApiResponse.fail(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("[Exception] : ", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생하였습니다.";
        return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, message);
    }

    private String extractErrorMessages(BindException ex) {
        return ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
    }
}
```

### 3단계: 서비스 로직에서 예외 발생시키기

서비스 코드에서는 `try-catch` 없이 필요한 상황에 예외를 던지기만 하면 됩니다.

```java
public ProductResponse getProductById(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_PRODUCT));

    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .description(product.getDescription())
        .price(product.getPrice())
        .createdAt(product.getCreatedAt())
        .build();
}
```

`GlobalExceptionHandler`가 예외를 가로채 아래와 같이 일관된 JSON 응답을 자동으로 생성합니다.

```json
{
  "error": {
    "errorCode": "NOT_FOUND_PRODUCT",
    "errorMessage": "상품을 찾을 수 없습니다."
  }
}
```

---

## 3. 전체 흐름 요약

```
Service → DomainException 발생
    │
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    │  @ExceptionHandler(DomainException.class)
    ▼
ApiResponse.fail(httpStatus, code, message)
    │
    ▼
Client ← { "error": { "errorCode": "...", "errorMessage": "..." } }
```

| 예외 타입 | 처리 메서드 | 상태 코드 |
|---------|-----------|---------|
| `DomainException` | `handleDomainException` | Enum에 정의된 코드 |
| `MethodArgumentNotValidException` | `handleMethodArgumentNotValidException` | `400` |
| `BindException` | `handleBindException` | `400` |
| `Exception` (그 외 모든 예외) | `handleException` | `500` |
