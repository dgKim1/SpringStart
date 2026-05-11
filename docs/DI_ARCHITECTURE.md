# 의존성(DI) 주입

## 생성자 주입 (Constructor Injection)

생성자를 통해 의존성을 주입받는 방식입니다. Spring 4.3 이후로는 생성자가 단 하나일 경우 `@Autowired`를 생략할 수 있습니다.

```java
@Service
public class ProductService {
    private final ProductRepository productRepository;

    // 생성자가 호출되는 시점에 의존성이 주입된다.
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

```java
@Service
@RequiredArgsConstructor // Lombok이 final 필드를 파라미터로 받는 생성자를 자동 생성
public class ProductService {
    private final ProductRepository productRepository;
}
```

**장점:**
- **불변성(Immutability)**: `final` 키워드를 사용하여 의존성을 변경 불가능하게 만들 수 있습니다.
- **의존성 명확성**: 생성자 시그니처에 필요한 의존성이 명확히 드러나, 객체가 생성될 때 모든 의존성이 주입됨을 보장합니다.
- **순환 참조 방지**: 순환 참조 발생 시 애플리케이션 시작 시점에 오류를 발생시켜 문제를 조기에 발견할 수 있습니다.
- **테스트 용이성**: 순수 Java 코드로 테스트할 때 의존성을 주입하기 매우 편리합니다.

---

## 요약 및 최종 권장

| 주입 방식 | 추천 여부 | 불변성 | 테스트 용이성 | 순환 참조 | 코드 간결성 |
|----------|----------|--------|------------|---------|-----------|
| 생성자 주입 | 강력 추천 ✅ | 보장 (`final`) | 매우 높음 | 감지 가능 | 보통 |
| 수정자 주입 | 선택적 사용 | 보장 안됨 | 보통 | 감지 어려움 | 김 |
| 필드 주입 | 비추천 ⚠️ | 보장 안됨 | 매우 낮음 | 감지 어려움 | 매우 높음 |

결론적으로, 불변성을 보장하고 테스트에 용이하며 의존관계를 명확하게 만들어주는 **생성자 주입 방식을 항상 기본으로 사용**하는 것이 가장 좋습니다. Lombok의 `@RequiredArgsConstructor`를 함께 사용하면 코드를 더 간결하게 만들 수 있습니다.
