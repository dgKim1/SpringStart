## 🔍 한눈에 보기

이커머스 상품 도메인을 JPA 엔티티로 모델링하는 과정입니다.
단순히 테이블을 클래스로 옮기는 것을 넘어, **캡슐화·연관관계·생명주기 관리**라는 세 가지 설계 원칙을 적용합니다.
`Category → Product → ProductOption`으로 이어지는 1:N 계층 구조를 구축하고,
`ProductStatus` Enum으로 상품의 판매 상태를 안전하게 제어합니다.

**사용된 주요 개념**
- **캡슐화(Encapsulation)** — 필드를 외부에서 직접 변경하지 못하게 막고, 의미 있는 메서드로만 수정
- **`@Enumerated(EnumType.STRING)`** — Enum 값을 숫자가 아닌 문자열로 DB에 저장
- **`CascadeType.ALL`** — 부모 엔티티 저장/삭제 시 자식 엔티티에도 동일하게 전파
- **`orphanRemoval = true`** — 부모 컬렉션에서 제거된 자식을 DB에서도 자동 삭제
- **연관관계 편의 메서드** — 양방향 관계에서 양쪽 객체 상태를 한 번에 동기화하는 메서드
- **자기 참조(Self-Referencing)** — 엔티티가 자기 자신을 부모로 참조하는 계층 구조

---

## 🪜 Step by Step 로직 설명

### Step 1: 상태 관리 — ProductStatus Enum

```java
public enum ProductStatus {
    FOR_SALE,
    STOP_SALE,
    OUT_OF_STOCK
}
```

```java
// Product.java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
ProductStatus status;
```

상품의 판매 상태를 `String` 타입 대신 Enum으로 정의합니다.

```sql
-- DB에 저장되는 값
status VARCHAR(20) NOT NULL DEFAULT 'FOR_SALE'
-- "FOR_SALE", "STOP_SALE", "OUT_OF_STOCK" 문자열 그대로 저장
```

💡 **`EnumType.ORDINAL`(기본값)을 쓰면 안 됩니다.** Ordinal은 선언 순서(0, 1, 2)를 저장하므로 나중에 Enum 항목을 추가하거나 순서를 바꾸면 기존 데이터가 전혀 다른 상태를 가리키게 됩니다. **항상 `EnumType.STRING`을 사용하세요.**

---

### Step 2: 캡슐화 — 비즈니스 메서드로만 수정

```java
// 외부에서 직접 필드를 바꿀 수 없음 (setter 없음)
// 비즈니스 의미가 드러나는 메서드로만 수정 가능

public void updateInfo(String name, String description, Integer price) {
    this.name = name;
    this.description = description;
    this.price = price;
}

public void changeStatus(ProductStatus status) {
    this.status = status;
}
```

`@Getter`만 선언하고 `@Setter`는 선언하지 않습니다. 데이터를 바꾸고 싶다면 반드시 `updateInfo()`, `changeStatus()` 같은 메서드를 통해야 합니다.

💡 **setter가 있으면 코드 어디서든 `product.setStatus("STOP_SALE")`처럼 상태를 바꿀 수 있습니다.** 메서드명에 비즈니스 의도(`changeStatus`)가 드러나면, 이 코드가 "판매 상태를 변경한다"는 맥락임을 바로 알 수 있습니다.

---

### Step 3: 영속성 전이(Cascade) + 고아 객체 제거

```java
// Product.java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
List<ProductOption> productOptions = new ArrayList<>();

public void addOption(ProductOption option) {
    productOptions.add(option);
}
```

```java
// 사용 예시
Product product = Product.builder()
    .name("반팔 티셔츠")
    .price(29000)
    .status(ProductStatus.FOR_SALE)
    .build();

ProductOption option = ProductOption.builder()
    .product(product)
    .name("빨강-M")
    .additionalPrice(0)
    .stock(50)
    .build();

product.addOption(option);
productRepository.save(product); // product와 option이 함께 INSERT됨
```

`CascadeType.ALL`로 인해 `product`를 저장하면 `productOptions` 리스트 안의 옵션들도 함께 INSERT됩니다. `productRepository.save(option)`을 별도로 호출할 필요가 없습니다.

```java
// 옵션 제거 시
product.getProductOptions().remove(option);
// orphanRemoval = true 이므로 트랜잭션 커밋 시 해당 option이 DB에서 DELETE됨
```

💡 **`CascadeType.ALL`과 `orphanRemoval`은 함께 쓰는 경우가 많지만 의미가 다릅니다.**
- `CascadeType.ALL`: Product를 저장/삭제할 때 ProductOption도 함께 저장/삭제
- `orphanRemoval`: 컬렉션에서 제거된 ProductOption을 DB에서도 삭제 (Product가 삭제되지 않아도)

---

### Step 4: 카테고리 자기 참조 + 편의 메서드

```java
// Category.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
Category parent;           // 부모 카테고리 (연관관계의 주인)

@OneToMany(mappedBy = "parent")
List<Category> children = new ArrayList<>();  // 자식 카테고리 목록

public void addChild(Category child) {
    child.parent = this;      // 1. DB에 반영되는 FK 설정 (연관관계 주인 쪽)
    this.children.add(child); // 2. 메모리의 children 리스트 동기화
}
```

```java
// 사용 예시
Category top = Category.builder().name("의류").build();
Category sub = Category.builder().name("상의").build();

top.addChild(sub);
// sub.parent = top (DB에 parent_id = top.id로 저장됨)
// top.children = [sub] (메모리 동기화)
```

💡 **`addChild()`가 없으면 양방향 관계에서 버그가 생깁니다.** `top.children.add(sub)`만 하고 `sub.parent = top`을 빠뜨리면, 메모리의 `top.children`에는 `sub`가 있지만 DB의 `parent_id`는 null입니다. 편의 메서드는 이 두 가지를 반드시 같이 처리합니다.

---

### Step 5: DB 제약 조건 — CHECK

```sql
-- price가 음수가 되지 않도록 DB 레벨에서 보호
ADD CONSTRAINT chk_product_price CHECK (price >= 0);

-- product_options 테이블
CONSTRAINT chk_additional_price CHECK (additional_price >= 0),
CONSTRAINT chk_option_stock     CHECK (stock >= 0)
```

애플리케이션 코드에서 유효성 검사를 하더라도, DB 레벨의 `CHECK` 제약은 **어떤 경로로 데이터가 들어와도** 음수 저장을 차단합니다. (직접 SQL 실행, 배치 작업 등 포함)

💡 **CHECK 제약은 JPA 엔티티에 표현되지 않습니다.** DB 스키마(Flyway 파일)에서만 관리됩니다. 엔티티의 `@Column`에는 `@Min(0)` 같은 Bean Validation 어노테이션을 추가하여 애플리케이션 레벨 검증과 병행하는 것이 좋습니다.

---

## 🧠 사용된 개념 정리

### CascadeType

**정의**
부모 엔티티에 수행하는 영속성 작업(저장, 삭제 등)을 연관된 자식 엔티티에도 전파하는 설정입니다.

**예제**
```java
// CascadeType 종류
CascadeType.PERSIST  // 저장 전파
CascadeType.REMOVE   // 삭제 전파
CascadeType.MERGE    // 병합 전파
CascadeType.ALL      // 위 모든 작업 전파

@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
List<ProductOption> productOptions = new ArrayList<>();
```

**이 코드에서의 역할**
`Product`를 저장하면 `productOptions` 리스트 안의 `ProductOption`도 함께 INSERT됩니다. `Product`를 삭제하면 연관된 모든 옵션도 함께 DELETE됩니다.

**어떤 기능에 많이 활용되나요?**
- 주문(Order)을 저장할 때 주문 상품(OrderProduct)도 함께 저장
- 게시글을 삭제할 때 댓글도 함께 삭제
- Aggregate Root(집합 루트)가 하위 객체의 생명주기를 완전히 관리할 때

---

### orphanRemoval

**정의**
부모 엔티티의 컬렉션에서 제거된 자식 엔티티를 DB에서도 자동으로 삭제하는 설정입니다.

**예제**
```java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
List<ProductOption> productOptions = new ArrayList<>();

// orphanRemoval = true 동작
product.getProductOptions().remove(0); // 리스트에서 제거
// 트랜잭션 커밋 시 → DELETE FROM product_options WHERE id = ?
```

**이 코드에서의 역할**
`product.getProductOptions().remove(option)` 한 줄만으로 DB에서도 해당 옵션이 삭제됩니다. `optionRepository.delete(option)`을 별도로 호출할 필요가 없습니다.

**어떤 기능에 많이 활용되나요?**
- 상품 옵션 수정 API: 기존 옵션을 모두 지우고 새로운 옵션으로 교체
- 댓글 관리: 특정 댓글만 선택적으로 삭제
- 항상 `CascadeType.ALL`(또는 최소 `PERSIST`)과 함께 사용

---

### 연관관계 편의 메서드

**정의**
양방향 연관관계에서 양쪽 객체의 참조를 한 번에 동기화해주는 메서드입니다. 한쪽만 설정하면 메모리 상태와 DB 상태가 달라지는 버그를 방지합니다.

**예제**
```java
// 편의 메서드 없이 직접 설정 시 실수하기 쉬움
sub.parent = top;        // DB에 반영 O (연관관계 주인)
top.children.add(sub);   // 메모리 동기화 O
// 둘 중 하나라도 빠지면 버그

// 편의 메서드로 묶으면 항상 안전
public void addChild(Category child) {
    child.parent = this;
    this.children.add(child);
}
```

**이 코드에서의 역할**
`top.addChild(sub)` 한 번의 호출로 `sub.parent`(DB FK)와 `top.children`(메모리 리스트) 양쪽을 동시에 설정합니다.

**어떤 기능에 많이 활용되나요?**
- 1:N 양방향 관계의 모든 엔티티 (User-Order, Product-ProductOption 등)
- 연관관계를 설정하는 로직이 여러 곳에서 호출될 때 일관성 보장

---

## 📝 정리

- **Enum은 항상 `EnumType.STRING`으로 저장한다.** Ordinal은 순서 변경에 취약하여 데이터 정합성이 깨질 수 있다.
- **setter 대신 비즈니스 메서드를 사용한다.** `changeStatus()`, `updateInfo()`처럼 의도가 드러나는 이름으로 수정 경로를 통제한다.
- **`CascadeType.ALL` + `orphanRemoval`은 Aggregate Root 패턴이다.** 부모가 자식의 생명주기 전체를 책임질 때 사용한다. 여러 곳에서 공유되는 엔티티에는 사용하면 안 된다.
- **양방향 관계에는 반드시 편의 메서드를 만든다.** 양쪽을 한 번에 설정하지 않으면 메모리와 DB 상태가 달라지는 버그가 발생한다.
- **DB 제약(CHECK)은 JPA 레벨과 별개로 관리한다.** Flyway 파일에 CHECK 제약을 추가하면 어떤 경로로 데이터가 들어와도 DB 레벨에서 보호된다.
