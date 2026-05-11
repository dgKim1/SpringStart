## 1. 단일 테이블 구조 설계 💡

단일 테이블 설계는 모든 데이터베이스 설계의 출발점입니다. 단순히 컬럼을 나열하는 것을 넘어, 데이터의 정체성과 규칙을 정의하는 과정입니다.

### 테이블 기본 구조와 PK 정의

- **핵심 개념: 대리 키(Surrogate Key) vs 자연 키(Natural Key)**
    - **대리 키 (권장)**: `id GENERATED ALWAYS AS IDENTITY PRIMARY KEY` 처럼 비즈니스와 직접적인 관련이 없는, 오직 데이터 식별만을
      위해 인위적으로 부여된 키입니다. **변경될 일이 없고 항상 고유함을 보장**하므로 현대 애플리케이션에서 PK로 사용하는 것을 강력히 권장합니다.
    - **자연 키**: 이메일이나 주민등록번호처럼 비즈니스 로직에 실제 존재하는 값으로 PK를 잡는 것입니다. 하지만 이런 값은 변경될 수 있거나 개인정보보호 이슈가 있어
      PK로 사용하기에 적합하지 않은 경우가 많습니다.
- **테이블 설계 예제 (**`products` **테이블)**

    ```sql
    CREATE TABLE products
    (
        id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name           VARCHAR(255)   NOT NULL,
        price          DECIMAL(10, 2) NOT NULL,
        stock_quantity INT            NOT NULL DEFAULT 0,
        created_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
        updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    ```

- **설계 포인트**
    - `id`는 대리 키(PK)로, 각 사용자를 고유하게 식별합니다.
    - `NOT NULL` 제약조건을 적절히 활용하여 데이터 무결성을 보장합니다.
    - 컬럼 이름은 명확하고 직관적으로 정의해야 유지보수성을 높일 수 있습니다.
    - 데이터 타입과 제약 조건(`NOT NULL`, `UNIQUE`)을 적절히 설정하여 데이터 무결성을 보장합니다.

---

## 2. 테이블 간 연관관계 설계 🔗

데이터의 중복을 최소화하고 일관성을 유지하기 위해 **정규화(Normalization)** 과정을 거쳐 테이블을 분리하고 관계를 맺습니다. 이 관계를 어떻게 맺느냐에 따라 시스템의
성능과 확장성이 결정됩니다.

### 1:N 관계 (One-to-Many)

하나의 엔티티가 여러 다른 엔티티와 관계를 맺는, 가장 흔하고 기본적인 관계입니다.

- 데이터 중복을 피하기 위해 1쪽에 해당하는 정보를 분리하고, N쪽에서 1쪽의 데이터를 참조하는 방식입니다. 예를 들어, 한 명의 사용자 정보를 여러 주문에 반복해서 저장하는
  대신, `user` 테이블을 만들고 `order` 테이블에서는 해당 사용자의 ID만 참조합니다.
- **예제**: 한 명의 사용자(1)는 여러 개의 주문(N)을 가질 수 있습니다.SQL

    ```sql
    -- orders 테이블
    CREATE TABLE orders
    (
        id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        user_id     BIGINT         NOT NULL,
        total_price DECIMAL(10, 2) NOT NULL,
        status      VARCHAR(20)    NOT NULL,
        created_at  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
        updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    ```

- **JPA 엔티티 매핑 예시**
    - **`User` (1쪽 엔티티)**: 자신이 가진 주문 목록을 관리합니다.

        ```java
        @Entit
        public class User {
            // ...
            // '나'는 Order 엔티티의 'user' 필드에 의해 매핑되었다.
            @OneToMany(mappedBy = "user") 
            List<Order> orders = new ArrayList<>();
        }
        ```

    - `Order` **(N쪽 엔티티, 연관관계의 주인)**: 자신이 속한 사용자를 명시합니다.

        ```java
        @Entity
        public class Order {
            // ...
            @ManyToOne(fetch = FetchType.LAZY) // Order(N) : User(1) 관계
            @JoinColumn(name = "user_id") // DB의 user_id 컬럼과 매핑
            User user;
        }
        ```

- **설계 포인트 및 고려사항**
    - **연관관계의 주인(Owner)**: 외래 키(`user_id`)를 관리하는 N쪽(`Order`)이 연관관계의 주인이 됩니다.

### N:N 관계 (Many-to-Many)

양쪽 엔티티가 서로에게 1:N 관계를 가지는 경우로, 데이터베이스에서는 직접 표현할 수 없어 중간에 **연결 테이블(Junction Table)**을 두어 1:N 관계 두 개로
풀어냅니다.

- '주문-상품' 관계처럼, 하나의 주문에 여러 상품이 포함되고 하나의 상품도 여러 주문에 포함될 수 있는 복잡한 관계를 표현합니다.
- **예제**: 하나의 주문(N) ↔ 여러 상품(N)SQL

    ```sql
    -- 연결 테이블: `order_products`
    CREATE TABLE order_products
    (
        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        order_id   BIGINT         NOT NULL, -- FK: 어떤 주문에 속하는지
        product_id BIGINT         NOT NULL, -- FK: 어떤 상품인지
        quantity   INT            NOT NULL, -- 주문 수량
        price      DECIMAL(10, 2) NOT NULL  -- 주문 시점의 상품 가격
    );
    ```

- **JPA 엔티티 매핑 예시 (권장 방식)**
    - **`@ManyToMany`는 실무에서 비추천**: 연결 테이블에 `quantity`, `price` 같은 추가 정보를 담을 수 없고, 복잡한 쿼리 작성이 어려워 거의
      사용하지 않습니다.
    - **연결 테이블을 별도의 엔티티로 승격**하여 1:N, N:1 관계로 풀어내는 것이 정석입니다.

    ```java
    @Entity
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Table(name = "order_products")
    public class OrderProduct{
        @Id 
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
    
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id")
        Order order;
    
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "product_id")
        Product product;
    
    		@Column
        Intger quantity;
        
        @Column
        BigDecimal price;
    }
    ```

- **설계 포인트 및 고려사항**
    - 연결 테이블은 단순히 관계만 맺는 것이 아니라, **관계 자체에 대한 의미 있는 데이터**(주문 수량, 주문 시점 가격 등)를 저장하는 중요한 역할을 합니다.

### 계층형 관계 (트리 구조)

카테고리, 댓글 등 계층 구조를 표현할 때 사용하며, 엔티티가 자기 자신을 참조하는 **자기 참조(Self-Referencing)** 관계를 통해 부모-자식 구조를 만듭니다.

- 조직도나 파일 시스템처럼 상위-하위 관계가 반복되는 데이터를 표현하는 데 사용됩니다. 가장 일반적인 구현 방식은 **인접 리스트(Adjacency List)** 모델입니다.
- **예제**: 카테고리 테이블 (인접 리스트 모델)SQL

    ```sql
    CREATE TABLE categories (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        parent_id BIGINT DEFAULT NULL -- FK: 부모 카테고리의 id를 참조
    );
    ```

- **JPA 엔티티 매핑 예시**

    ```java
    @Entity
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Table(name = "categories")
    public class Category {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
    
        String name;
    
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "parent_id")
        Category parent; // 부모 카테고리
    
        @OneToMany(mappedBy = "parent")
        List<Category> children = new ArrayList<>(); // 자식 카테고리 목록
    }
    ```

- **설계 포인트 및 고려사항**
    - **장점**: 구조가 매우 직관적이고 데이터를 추가하거나 수정하기 쉽습니다.
    - **단점**: 특정 노드의 모든 하위 노드를 조회하려면 여러 번의 재귀적인 쿼리가 필요하여, 트리의 깊이가 깊어질수록 조회 성능이 급격히 저하될 수 있습니다.
    - **대안 모델**: 대용량 계층형 데이터를 다룰 때는 조회 성능이 더 좋은 **Nested Set**이나 **Materialized Path** 같은 다른 모델을
      고려하기도 합니다. (이는 더 심화된 주제입니다.)

### FOREIGN KEY 제약조건: 써야 할까, 말아야 할까? 🤔

제공해주신 내용은 `FOREIGN KEY`를 사용하지 않는 이유에 초점이 맞춰져 있습니다. 이는 대규모 트래픽 서비스나 MSA 환경에서 주로 논의되는 내용으로, 장단점을 균형 있게
이해하는 것이 중요합니다.

**✅ `FOREIGN KEY`를 사용하는 이유 (장점) - 데이터 무결성 최우선**

- **데이터 무결성 보장 (가장 큰 장점)**: 데이터베이스 레벨에서 참조 관계의 유효성을 **강제로 보장**합니다. 예를 들어, 존재하지 않는 `user_id`로 주문을
  생성하는 것을 원천적으로 차단하여 '고아 데이터' 발생을 막습니다.
- **애플리케이션 개발 단순화**: 데이터 정합성을 DB에 위임하므로, 애플리케이션 코드에서 모든 예외 케이스를 처리해야 하는 부담이 줄어듭니다.
- **명시적인 관계 정의**: 테이블 구조만 봐도 데이터 간의 관계를 명확하게 파악할 수 있어 문서의 역할을 합니다.

**⚠️ `FOREIGN KEY`를 사용하지 않는 이유 (단점) - 유연성과 성능 최우선**

- **성능 오버헤드**: `INSERT`, `UPDATE`, `DELETE` 시마다 참조 무결성을 확인하는 비용이 발생합니다. 초당 수천, 수만 건의 쓰기 작업이 발생하는
  시스템에서는 이 오버헤드가 병목이 될 수 있습니다.
- **개발 및 마이그레이션 복잡성**: 테이블 변경이나 데이터 이전 작업 시, 참조 순서를 고려해야 하므로 작업이 복잡해지고 제약이 많아집니다.
- **분산 시스템(MSA)에서의 한계**: 서비스별로 데이터베이스가 분리된 마이크로서비스 아키텍처에서는 DB 레벨의 `FOREIGN KEY`를 물리적으로 사용할 수 없습니다.

**상황에 맞는 선택이 중요**

- **일반적인 웹 애플리케이션 / 모노리스 아키텍처**: **`FOREIGN KEY` 사용을 고민해볼만합니다.** 성능 저하보다 데이터 무결성을 보장하는 이점이 훨씬 큽니다.
- **대규모 트래픽 서비스 / MSA**: `FOREIGN KEY`를 사용하지 않고, **애플리케이션 레벨에서 코드(ORM, 로직)로 데이터의 정합성을 관리**하는 전략을
  선택하는 경우가 많습니다. 이는 시스템의 확장성과 유연성을 극대화하기 위한 선택입니다.

---

## 3. 데이터 정규화의 현대적 접근 💡

정규화는 데이터베이스 설계 시 **데이터의 중복을 최소화하고 무결성을 보장**하기 위해 테이블을 구조화하는 체계적인 과정입니다. 정규화를 통해 '데이터 이상 현상(Anomaly)'
을 방지하고, 더 효율적이고 논리적인 데이터 모델을 만들 수 있습니다.

### 정규화의 핵심 목표: 데이터 이상 현상(Anomaly) 방지

만약 정규화 없이 하나의 테이블에 모든 정보를 저장하면 다음과 같은 문제가 발생합니다.

| **주문번호** | **고객ID** | **고객이름** | **고객등급** | **상품명** | **상품가격** |
|----------|----------|----------|----------|---------|----------|
| 1001     | user01   | 김철수      | GOLD     | 노트북     | 1500000  |
| 1002     | user02   | 이영희      | SILVER   | 키보드     | 50000    |
| 1003     | user01   | 김철수      | GOLD     | 마우스     | 30000    |

- **갱신 이상 (Update Anomaly)**: '김철수'의 등급이 'VIP'로 변경되면, 1001번과 1003번 주문의 '고객등급'을 모두 바꿔야 합니다. 하나라도 누락되면
  데이터가 불일치하게 됩니다.
- **삽입 이상 (Insertion Anomaly)**: 아직 주문을 하지 않은 신규 고객 '박민준'을 등록하려면, 불필요한 주문번호나 상품명까지 `NULL`로 넣어야 하는
  문제가 발생합니다.
- **삭제 이상 (Deletion Anomaly)**: '이영희' 고객이 1002번 주문을 취소하여 해당 데이터가 삭제되면, '이영희'라는 고객 정보 자체가 DB에서 사라져
  버립니다.

**정규화는 이러한 문제를 해결하기 위해 테이블을 논리적인 단위로 분해하는 과정입니다.**

### 핵심 정규형 (1NF, 2NF, 3NF) 쉽게 이해하기

이론적으로는 5NF, BCNF 등 여러 단계가 있지만, 실무에서는 **제3정규형(3NF)까지 만족시키는 것을 목표**로 하는 경우가 대부분입니다.

- **제1정규형 (1NF): 모든 컬럼은 원자적(Atomic) 값을 가져야 한다.**
    - **의미**: 한 칸에 여러 개의 값이 들어갈 수 없습니다.
    - **예시**: `취미` 컬럼에 '농구, 축구'처럼 쉼표로 구분된 값을 넣으면 1NF 위반입니다.
    - **해결**: 별도의 `취미` 테이블을 만들거나, 각 취미를 별개의 행으로 분리해야 합니다.

- **제2정규형 (2NF): 부분 함수 종속을 제거한다. (PK가 여러 컬럼일 때 적용)**
    - **의미**: 테이블의 모든 컬럼은 **반드시 기본 키(PK) 전체에 종속**되어야 합니다. PK의 일부에만 종속되어서는 안 됩니다.
    - **예시**: `(주문번호, 상품번호)`가 PK인 테이블에서 `상품명`은 `상품번호`에만 종속됩니다. `주문번호`와는 관계가 없습니다. 이것이 '부분 함수 종속'입니다.
    - **해결**: `상품명`을 별도의 `상품` 테이블로 분리합니다. (`order_products`과 `product`의 관계)

- **제3정규형 (3NF): 이행 함수 종속을 제거한다.**
    - **의미**: 기본 키(PK)가 아닌 컬럼들끼리 서로 의존해서는 안 됩니다.
    - **예시**: `주문` 테이블에 `고객ID`, `고객이름`, `고객등급`이 모두 있다면, `고객이름`과 `고객등급`은 PK인 `주문ID`가 아닌 `고객ID`에 의해
      결정됩니다. 이것이 '이행 함수 종속'입니다.
    - **해결**: `고객이름`, `고객등급`을 별도의 `고객` 테이블로 분리합니다. (`order`와 `user`의 관계)

위의 1, 2, 3차 정규화를 거치면 맨 처음의 비정규화 테이블은 `users`, `products`, `orders`, `order_products`와 같이 논리적으로 잘 분리된
테이블 구조를 갖추게 됩니다.

### 현대적 관점: 정규화와 반정규화(Denormalization)의 줄다리기

"정규화 수준이 높을수록 무조건 좋은 설계인가?" **현대적인 관점에서는 꼭 그렇지만은 않습니다.**

- **반정규화(Denormalization)란?**
    - 데이터 조회 성능을 향상시키기 위해 **의도적으로 정규화 원칙을 위배**하여, 테이블에 중복 데이터를 추가하거나 테이블을 병합하는 과정입니다.

| **구분** | **정규화 (Normalization)**          | **반정규화 (Denormalization)**            |
|--------|----------------------------------|---------------------------------------|
| **장점** | 데이터 무결성 극대화, 중복 최소화, 데이터 모델 유연   | **조회 성능 향상 (JOIN 감소)**, 쿼리 단순화        |
| **단점** | 여러 테이블을 `JOIN`해야 하므로 조회 성능 저하 가능 | 데이터 중복 발생, 데이터 불일치 위험 증가, 입력/수정/삭제 복잡 |

- **언제 반정규화를 고려하는가?**
    - **읽기 작업이 압도적으로 많을 때**: 통계, 대시보드처럼 조회가 빈번한데, 매번 여러 테이블을 `JOIN`하는 비용이 너무 클 경우.
    - **미리 계산된 값이 필요할 때**: 게시물의 `좋아요 수`나 `댓글 수`처럼 자주 필요한 값을 매번 `COUNT(*)`로 계산하는 대신, 게시물 테이블에
      `like_count`, `comment_count` 컬럼을 두어 관리합니다.

### 실용적인 데이터베이스 설계 전략

현대적인 데이터베이스 설계는 정규화를 맹목적으로 따르기보다, **상황에 맞게 정규화와 반정규화를 조율하는 실용적인 접근**을 취합니다.

1. **기본은 정규화**: 우선 **제3정규형(3NF)을 목표로** 데이터 모델을 설계하여 데이터 무결성과 유연성을 확보합니다.
2. **성능 측정**: 애플리케이션을 개발하고 실제 사용 시나리오에 맞춰 성능을 측정합니다.
3. **병목 지점 식별**: 특정 조회 쿼리에서 `JOIN`으로 인한 성능 저하가 명확하게 **증명되면**,
4. **전략적 반정규화**: 해당 부분에만 **제한적으로 반정규화**를 적용하여 성능을 최적화합니다.

<aside>
💡

**"일단 정규화하고, 성능 문제가 발생하면 측정하여 필요한 곳만 반정규화하라 (Normalize first, denormalize where proven necessary)"**
가 현대적인 데이터 설계의 핵심 원칙입니다.

</aside>

---

## 4. DB 테이블 설계: 기본 구조와 관계 🛍️

온라인 쇼핑몰의 핵심 기능인 사용자, 상품, 주문을 처리하기 위한 데이터베이스 스키마 예제입니다. 이 설계는 데이터 중복을 최소화하고 일관성을 유지하는 정규화 원칙을 따릅니다.

### 핵심 엔티티: `user`와 `product`

모든 상거래의 기본이 되는 '누가(user)' '무엇을(product)' 사고파는지 정의하는 테이블입니다.

- **`users` 테이블**
    - **역할**: 회원가입한 고객의 정보를 저장합니다.
    - **주요 설계 포인트**:
        - `email`에 **UNIQUE** 제약조건을 설정하여 중복 가입을 방지합니다.
        - `password_hash`처럼 비밀번호는 절대 원본 그대로 저장하지 않고, 반드시 해시(hash)하여 저장해야 합니다.
- **`products` 테이블**SQL
    - **역할**: 판매하는 상품의 상세 정보를 저장합니다.
    - **주요 설계 포인트**:
        - `stock` 컬럼을 통해 상품의 재고를 관리하며, 주문 시 이 값을 감소시키는 로직이 필요합니다.
        - `price`는 현재 시점의 상품 판매 가격을 의미합니다.

    ```sql
    -- user Table
    CREATE TABLE users
    (
        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name       VARCHAR(50)  NOT NULL,
        email      VARCHAR(255) NOT NULL UNIQUE,
        password   VARCHAR(255) NOT NULL,
        created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    
    -- product Table
    CREATE TABLE products
    (
        id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        category_id BIGINT,
        name        VARCHAR(255)   NOT NULL,
        description TEXT,
        price       DECIMAL(10, 2) NOT NULL,
        stock       INT            NOT NULL DEFAULT 0,
        created_at  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
        updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    ```

### 계층 구조 데이터: `categories`

상품을 효율적으로 분류하고 탐색할 수 있도록 계층 구조를 가진 카테고리 정보를 저장합니다.

- **`categories` 테이블**
    - **역할**: '상의 > 티셔츠 > 반팔티' 와 같은 계층형 카테고리 구조를 표현합니다.
    - **주요 설계 포인트**:
        - `parent_id`라는 **자기 참조(Self-Referencing)** 외래 키를 사용합니다.
        - `parent_id`가 `NULL`이면 최상위 카테고리를 의미합니다.

    ```sql
    -- categories Table
    CREATE TABLE categories
    (
        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name       VARCHAR(255) NOT NULL,
        parent_id  BIGINT                DEFAULT NULL, -- FK: 부모 카테고리의 id를 참조
        created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    ```

### 주문 및 관계 설계: `orders` 와 `order_products`

주문 행위는 '어떤 주문'인지에 대한 정보와 '주문에 어떤 상품들이 담겼는지'에 대한 정보로 나뉩니다. 이는 **1:N**과 **N:N** 관계를 모두 포함하는 핵심적인
설계입니다.

- **`orders` 테이블 (1:N 관계)**
    - **역할**: 한 건의 주문 자체에 대한 종합 정보(주문자, 배송지, 총액, 상태)를 저장합니다.
    - **주요 설계 포인트**:
        - `user_id`를 통해 `user` 테이블과 **1:N 관계**를 맺습니다. (한 명의 유저는 여러 번 주문할 수 있습니다.)
        - `status` 컬럼을 통해 '주문 처리중', '배송 완료' 등 주문의 생명주기(Lifecycle)를 관리합니다.
- **`order_products` 테이블 (N:N 관계 해결)**SQL
    - **역할**: `orders`와 `products` 사이의 **N:N 관계를 해결**하기 위한 **연결 테이블(Junction Table)**입니다. 한 주문에 어떤
      상품들이 몇 개씩, 얼마에 팔렸는지를 기록합니다.
    - **주요 설계 포인트**:
        - `order_id`와 `product_id`를 외래 키로 가져 관계를 맺습니다.
        - **가장 중요한 점**: `price` 컬럼이 `product` 테이블에도 있지만 여기에도 존재합니다. 이는 상품 가격이 미래에 변동되더라도, **주문이 일어난
          시점의 가격을 역사 기록으로 저장**하기 위함입니다.

    ```sql
    CREATE TABLE orders
    (
        id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        user_id     BIGINT         NOT NULL,
        total_price DECIMAL(10, 2) NOT NULL,
        status      VARCHAR(20)    NOT NULL,
        created_at  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
        updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    
    CREATE TABLE order_products
    (
        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        order_id   BIGINT         NOT NULL, -- FK: 어떤 주문에 속하는지
        product_id BIGINT         NOT NULL, -- FK: 어떤 상품인지
        quantity   INT            NOT NULL, -- 주문 수량
        price      DECIMAL(10, 2) NOT NULL  -- 주문 시점의 상품 가격
    );
    ```

### 전체 구조 및 관계 요약

```sql
[ users ]--<1:N>--[ orders ]--<1:N>--[ order_products ]--<N:1>--[ products ]
                                                                      |
                                                                      |
                                                                    <N:1>
                                                                      |
                                                                 [ categories ]
                                                                      |
                                                                    <N:1> (자기참조)
                                                                      |
                                                                  (parent_id)
```

- **users : orders** = 1 : N
- **orders : order_products** = 1 : N
- **products : order_products** = 1 : N
- **categories : products** = 1 : N
- **categories : categories** = 1 : N (자기 참조)

---

## 5. @ManyToOne과 @OneToMany: 핵심 개념 이해하기 💡

두 어노테이션은 JPA에서 가장 중요하고 또 가장 헷갈리는 부분입니다. 자동차의 '엔진'과 '바퀴'처럼, 이 둘의 관계를 정확히 이해해야 제대로 된 애플리케이션을 만들 수
있습니다.

### 1단계: 가장 간단한 관계부터 시작하기 (`@ManyToOne` 단방향)

먼저, 데이터베이스와 가장 가까운 **`@ManyToOne`**부터 생각해보겠습니다.

- **상황**: 여러 명의 '학생(Student)'이 하나의 '교실(Classroom)'에 속해있는 상황을 떠올려보세요.
- **데이터베이스**: `student` 테이블에는 `classroom_id` 라는 외래 키(FK)가 있어서, 어떤 학생이 몇 반 소속인지 알 수 있습니다.
- **JPA 엔티티**: 이 관계를 코드로 표현하면 다음과 같습니다.Java

    ```java
    // Student.java (N쪽)
    @Entity
    public class Student {
        @Id
        private Long id;
        private String name;
    
        @ManyToOne // 학생(N) : 교실(1) 관계
        @JoinColumn(name = "classroom_id") // DB의 classroom_id 컬럼과 연결
        private Classroom classroom;
    }
    
    // Classroom.java (1쪽)
    @Entity
    public class Classroom {
        @Id
        private Long id;
        private String name;
    
        // 아직 학생에 대한 정보가 없다.
    }
    ```

- `Student` 엔티티는 `classroom` 필드를 통해 자신이 어느 교실 소속인지 알 수 있습니다. 하지만 반대로 `Classroom` 엔티티는 어떤 학생들이 있는지 아직
  모릅니다. 이처럼 **한쪽만 관계를 아는 것을 '단방향 관계**'라고 합니다.

### 2단계: 서로를 알게 하기 (`@OneToMany` 추가와 '연관관계의 주인')

이제 '교실' 쪽에서도 어떤 '학생'들이 있는지 알고 싶게 되었습니다. 이때 **`@OneToMany`**가 등장합니다.

```java
// Classroom.java (1쪽) - 수정
@Entity
public class Classroom {

  @Id
  private Long id;
  private String name;

  @OneToMany(mappedBy = "classroom") // 1 : 학생(N) 관계
  private List<Student> students = new ArrayList<>();
}
```

이제 `Classroom` 객체도 `students` 리스트를 통해 소속 학생들을 알 수 있게 되었습니다. 하지만 여기서 JPA의 가장 중요한 질문이 나옵니다.

> "JPA 입장에서, 대체 누가 진짜 외래 키(FK)를 관리해야 할까요?"
>

`Student`의 `classroom` 필드를 바꿔야 할까요, 아니면 `Classroom`의 `students` 리스트에 학생을 추가하거나 빼야 할까요? 두 군데서 모두 관계를
설정할 수 있으니 혼란스럽습니다.

### ⭐️ 가장 중요한 개념: 연관관계의 주인(Owner)과 `mappedBy`

JPA는 이 혼란을 해결하기 위해 '**연관관계의 주인**'이라는 개념을 도입했습니다.

- **연관관계의 주인이란?**
    - 실제로 데이터베이스의 **외래 키(FK)를 관리(등록, 수정)할 권한을 가진 쪽**입니다.
    - 주인은 단 한쪽만 될 수 있습니다.
    - **누가 주인이 되는가?** 외래 키가 있는 곳, 즉 **N쪽(`@ManyToOne`이 있는 곳)**이 항상 주인이 됩니다.
    - 우리 예제에서는 `student` 테이블에 `classroom_id`가 있으므로, **`Student` 엔티티가 주인**입니다.

- **`mappedBy`의 진짜 의미**
    - 주인이 아닌 쪽(`Classroom`)은 `@OneToMany`에 `mappedBy` 속성을 반드시 적어야 합니다.
    - `@OneToMany(mappedBy = "classroom")` 을 한국어로 풀어쓰면 다음과 같습니다.

      > "나는 연관관계의 주인이 아니야. 나는 Student 엔티티 안에 있는 classroom 필드에 의해 관리되고 있어. 그러니 내 리스트에 학생을 추가하거나 빼도
      DB에 반영하지 말고, 오직 저쪽(Student)에서 관계가 변경될 때 거울처럼 내 상태를 갱신만 해줘."
  >
    - 즉, **`mappedBy`는 주인이 아님을 나타내는 '꼬리표'** 이자, 관계를 관리하는 실제 필드가 무엇인지 알려주는 '연결고리'입니다.

### 3단계: 처음 배울 때 꼭 지켜야 할 규칙 (실수 방지)

1. **양방향 관계에서는 '연관관계 편의 메서드'를 만드세요.**
    - `Student`가 주인이므로, `student.setClassroom(classroom)`을 호출해야 DB에 반영됩니다.
    - 하지만 객체지향적으로는 `classroom.getStudents().add(student)`도 말이 됩니다. 두 코드 중 하나만 실행하면 객체 상태와 DB 상태가
      달라지는 문제가 생깁니다.
    - **해결책**: 양쪽의 관계를 한 번에 설정해주는 메서드를 만듭니다.

    ```java
    // Classroom.java 에 추가
    public void addStudent(Student student) {
        this.students.add(student); // 1. 교실의 학생 리스트에 추가
        student.setClassroom(this); // 2. 학생의 소속 교실을 여기로 설정 (주인에게 변경 알림)
    }
    ```

2. **무한 루프를 조심하세요 (`toString()`, JSON 변환)**
    - `Classroom`의 `toString()`을 호출하면 `students` 리스트를 출력하고, 리스트 안의 각 `Student`는 다시 `toString()`을
      호출하여 `classroom`을 출력하고... 이렇게 무한 반복됩니다.
    - **해결책**: Lombok의 `@ToString` 사용 시, `@ToString.Exclude`를 양방향 관계 필드 중 하나에 붙여서 순환을 끊어야 합니다. JSON
      변환 시에는 `@JsonManagedReference` / `@JsonBackReference`를 사용합니다.

---

## 6. 엔티티 연관관계 매핑 🧬

데이터베이스 테이블 간의 관계를 JPA 엔티티의 객체 연관관계로 매핑하는 방법을 완전한 예제 코드와 함께 심층적으로 알아봅니다.

### 1:N 양방향 관계: `User` ↔ `order`

한 명의 사용자(`User`)가 여러 개의 주문(`order`)을 가질 수 있는, 가장 일반적인 관계입니다.

- **관계**: `Users`와 `orders`는 1:N 관계입니다. 데이터베이스에서는 N쪽인 `orders` 테이블이 `user_id`라는 외래 키(FK)를 가집니다.
- **연관관계의 주인 (Owner)**: JPA에서는 이 외래 키를 직접 관리하는 엔티티를 '주인'으로 정합니다. 이 경우, `order` 엔티티의 `user` 필드가
  `@JoinColumn(name = "user_id")`를 통해 `user_id` 컬럼과 직접 매핑되므로, **`order`가 연관관계의 주인**이 됩니다.
- **주인이 아닌 쪽 (Non-owner)**: `User` 엔티티의 `orders` 필드는 `mappedBy` 속성을 통해 "나는 `order` 엔티티의 `user` 필드에
  의해 매핑된, 읽기 전용 관계다"라고 명시합니다. 이 필드를 통해 `User` 객체에서 관련 `order` 목록을 쉽게 조회할 수 있지만, 이 필드를 수정한다고 해서
  `order` 테이블의 `user_id`가 변경되지는 않습니다.

**주요 어노테이션 설명**

- **`@ManyToOne`**: N:1 관계를 나타냅니다.
- **`@OneToMany`**: 1:N 관계를 나타냅니다.
- **`@JoinColumn(name = "...")`**: 외래 키 컬럼을 직접 지정합니다. 연관관계의 주인이 사용합니다.
- **`@JsonManagedReference` / `@JsonBackReference`**: 양방향 관계의 엔티티를 JSON으로 변환할 때, `User`는 `order`를
  부르고, `order`는 다시 `User`를 부르는 **무한 순환 참조**가 발생합니다. 이를 방지하기 위해 1쪽(`User`)에 `@JsonManagedReference`를,
  N쪽(`order`)에 `@JsonBackReference`를 붙여 N쪽의 정보는 JSON 직렬화에서 제외합니다.

```java
// User.java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, length = 50)
  String name;

  @Column(nullable = false, unique = true)
  String email;

  @Column(nullable = false)
  String password;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  LocalDateTime updatedAt;

  @Builder
  public User(
      String name,
      String email,
      String password
  ) {
    this.name = name;
    this.email = email;
    this.password = password;
  }
}

// Order.java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column(nullable = false)
  BigDecimal totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  OrderStatus status;

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public Order(
      User user,
      BigDecimal totalPrice,
      OrderStatus status
  ) {
    this.user = user;
    this.totalPrice = totalPrice;
    this.status = status;
  }

}

// OrderStatus.java (Enum)
public enum OrderStatus {
  PENDING,
  COMPLETED,
  CANCELED
}

```

### N:1 단방향 및 자기 참조 관계: `Product` ↔ `Category`

여러 상품(`Product`)이 하나의 카테고리(`Category`)에 속하고, 카테고리는 자기 자신을 부모로 참조하여 계층 구조를 이루는 관계입니다.

- **N:1 (Product → Category)**: 가장 단순하고 직관적인 단방향 관계입니다. `Product` 엔티티가 `Category`의 참조를 가지고 있으며,
  데이터베이스에서는 `product` 테이블이 `category_id` 외래 키를 가집니다.
- **자기 참조 (Category → Category)**: `Category` 엔티티 내부에 또 다른 `Category` 타입의 `parent` 필드와 `children`
  리스트를 두어 부모-자식 관계를 표현합니다. 이는 1:N 양방향 관계의 특수한 형태입니다.

```java
// Product.java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  Category category;

  @Column(nullable = false)
  String name;

  @Column(columnDefinition = "TEXT")
  String description;

  @Column(nullable = false)
  BigDecimal price;

  @Column(nullable = false)
  Integer stock;

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public Product(
      Category category,
      String name,
      String description,
      BigDecimal price,
      Integer stock
  ) {
    this.category = category;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
  }
}

// Category.java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "categories")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false)
  String name;

  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  Category parent;

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public Category(
      String name,
      Category parent
  ) {
    this.name = name;
    this.parent = parent;
  }

}
```

### N:N 관계: `order` ↔ `Product`

하나의 주문(`order`)에는 여러 상품(`Product`)이, 하나의 상품은 여러 주문에 포함될 수 있습니다. 이 관계는 중간 **연결 엔티티(`orderProducts`)**
를 통해 두 개의 1:N 관계로 풀어냅니다.

- **`@ManyToMany`의 한계**: JPA는 `@ManyToMany` 어노테이션으로 N:N 관계를 직접 매핑할 수 있지만, 이는 연결 테이블을 JPA가 내부적으로만
  사용하여 개발자가 제어할 수 없습니다.
- **연결 엔티티(Link Entity) 패턴**: `orderProducts`이라는 별도의 엔티티를 만들어 N:N 관계를 명시적으로 풀어냅니다. `orderProducts`은
  `order`와 `Product`에 대해 각각 N:1 관계를 가집니다. 이 패턴을 통해 연결 테이블을 완벽하게 제어할 수 있습니다.
- **데이터의 역사성 보존**: `orderProducts`에 `price` 필드를 두는 것은 매우 중요합니다. 상품의 가격은 언제든 변할 수 있지만, 주문 내역에는 **결제가
  이루어진 시점의 가격**이 정확히 기록되어야 하기 때문입니다.

```java
// OrderProducts.java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "order_products")
public class OrderProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  Product product;

  @Column(nullable = false)
  Integer quantity;

  @Column(nullable = false)
  BigDecimal price;

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public OrderProduct(
      Order order,
      Product product,
      Integer quantity,
      BigDecimal price
  ) {
    this.order = order;
    this.product = product;
    this.quantity = quantity;
    this.price = price;
  }
}
```