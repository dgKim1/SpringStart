## 🔍 한눈에 보기

JPA는 연관관계로 묶인 엔티티를 **언제 DB에서 가져올지** 개발자가 선택할 수 있습니다.
기본 전략인 지연 로딩(Lazy)은 필요한 순간에만 쿼리를 실행해 효율적이지만,
반복문 안에서 연관 데이터를 접근하면 쿼리가 N번 폭발하는 **N+1 문제**가 발생합니다.
이를 해결하는 핵심 도구가 **Fetch Join**으로, 한 번의 쿼리로 연관 데이터를 함께 가져옵니다.

**사용된 주요 개념**
- `FetchType.LAZY` — 연관 엔티티를 실제 사용 시점까지 로딩을 미루는 전략
- `FetchType.EAGER` — 연관 엔티티를 항상 즉시 함께 조회하는 전략
- **N+1 문제** — 목록 조회(1번) 후 각 항목의 연관 데이터를 개별 조회(N번)하는 쿼리 폭발 현상
- **Fetch Join** — JPQL의 `JOIN FETCH`로 연관 데이터를 1번 쿼리에 함께 조회하는 기법
- **프록시(Proxy)** — Lazy 로딩 시 실제 객체 대신 JPA가 반환하는 가짜 객체
- `@BatchSize` — 연관 데이터를 `IN` 절로 묶어 쿼리 수를 줄이는 전략

---

## 🪜 Step by Step 로직 설명

### Step 1: 기본 설정 — 모든 연관관계는 LAZY로

```java
// Order.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
User user;

// User.java
@OneToMany(mappedBy = "user")
List<Order> orders = new ArrayList<>();
```

`@ManyToOne`의 기본값은 `EAGER`이므로 반드시 명시적으로 `LAZY`를 설정해야 합니다.
`@OneToMany`는 기본값이 이미 `LAZY`입니다.

💡 **`@ManyToOne`은 기본값이 EAGER**입니다. 명시하지 않으면 자동으로 즉시 로딩이 되어 의도치 않은 쿼리가 발생합니다.

---

### Step 2: N+1 문제 발생 확인

```java
// 쿼리 1번 발생
List<User> users = userRepository.findAll();

for (User user : users) {
    // 유저마다 쿼리 1번씩 추가 발생 → N번
    user.getOrders().size();
}
// 총 쿼리: 1 + N번
```

`findAll()`은 `users` 테이블만 조회합니다. 각 `User`의 `orders`는 아직 **프록시 상태**이고, `getOrders()`를 호출하는 순간 그때서야 `WHERE user_id = ?` 쿼리가 발생합니다. 유저가 100명이면 101번의 쿼리가 실행됩니다.

```
실제 실행되는 SQL 로그:
Hibernate: select ... from users                            ← 1번
Hibernate: select ... from orders where user_id=?          ← 유저1
Hibernate: select ... from orders where user_id=?          ← 유저2
Hibernate: select ... from orders where user_id=?          ← 유저3
```

💡 **N+1은 LAZY 때문이 아닙니다.** LAZY 자체는 올바른 전략입니다. 문제는 "필요한 데이터를 미리 알고 있는데도 명시적으로 함께 조회하지 않은 것"입니다.

---

### Step 3: Fetch Join으로 해결

```java
// UserRepository.java
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithOrders();
```

```java
// 쿼리 1번으로 모두 해결
List<User> users = userRepository.findAllWithOrders();

for (User user : users) {
    user.getOrders().size(); // 추가 쿼리 없음
}
```

`JOIN FETCH`는 SQL의 `INNER JOIN`처럼 `users`와 `orders`를 한 번에 가져옵니다. 이미 메모리에 올라와 있으므로 반복문 안에서 추가 쿼리가 발생하지 않습니다.

```sql
-- 실제 실행되는 SQL (1번)
SELECT DISTINCT u.*, o.*
FROM users u
INNER JOIN orders o ON u.id = o.user_id
```

💡 **`DISTINCT`가 필수입니다.** 유저 1명이 주문 2건을 가지면 JOIN 결과가 2행이 되어, DISTINCT 없이는 같은 User 객체가 리스트에 2번 담깁니다.

---

### Step 4: 테스트로 검증

```java
@SpringBootTest
@Transactional
class NPlus1ProblemTest {

    @BeforeEach
    void setUp() {
        // 유저 3명, 각 유저당 주문 2건 저장
        em.flush();
        em.clear(); // 1차 캐시 초기화 → 실제 DB 조회 유도
    }

    @Test
    void nPlus1_문제_발생() {
        List<User> users = userRepository.findAll();       // 쿼리 1번
        users.forEach(u -> u.getOrders().size());          // 쿼리 3번
        // 총 4번
    }

    @Test
    void fetchJoin_해결() {
        List<User> users = userRepository.findAllWithOrders(); // 쿼리 1번
        users.forEach(u -> u.getOrders().size());              // 추가 쿼리 없음
        // 총 1번
    }
}
```

`em.clear()`로 1차 캐시를 비워야 실제 DB 조회가 일어나 쿼리 로그를 정확히 확인할 수 있습니다.

💡 **`@Transactional`이 없으면 Lazy 로딩 시 `LazyInitializationException`이 발생합니다.** 트랜잭션 범위 밖에서 프록시를 초기화할 수 없기 때문입니다.

---

## 🧠 사용된 개념 정리

### 프록시 (Proxy)

**정의**
Lazy 로딩 시 JPA가 실제 객체 대신 반환하는 가짜 객체입니다. 실제 데이터는 없고, 처음 데이터에 접근하는 순간 DB 쿼리를 실행해 실제 객체로 초기화됩니다.

**예제**
```java
// LAZY 설정 시
Order order = em.find(Order.class, 1L); // orders만 조회
User proxy = order.getUser();           // 프록시 반환 (쿼리 없음)

System.out.println(proxy.getClass());   // class com.sparta...User$HibernateProxyXXX
System.out.println(proxy.getName());    // 이 시점에 SELECT * FROM users WHERE id = ? 실행
```

**이 코드에서의 역할**
`findAll()`로 가져온 각 `User`의 `orders` 필드는 프록시 상태입니다. `getOrders()`를 호출할 때 비로소 각각의 쿼리가 실행되어 N+1이 발생합니다.

**어떤 기능에 많이 활용되나요?**
- 상세 페이지처럼 연관 데이터가 반드시 필요한 경우에는 Fetch Join과 함께 사용
- 목록 페이지처럼 연관 데이터가 불필요한 경우 Lazy 그대로 유지해 불필요한 조회 방지
- `@Transactional` 범위 안에서만 프록시 초기화 가능

---

### Fetch Join

**정의**
JPQL에서 연관 엔티티를 SQL의 JOIN처럼 한 번에 가져오는 명시적 조회 기법입니다. 일반 JOIN과 달리 연관 엔티티도 영속성 컨텍스트에 함께 올립니다.

**예제**
```java
// 일반 JOIN: orders는 SELECT에 포함되지 않아 Lazy 상태 유지
@Query("SELECT u FROM User u JOIN u.orders o")
List<User> findAllWithJoin();

// Fetch JOIN: orders까지 한 번에 로딩
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithFetchJoin();
```

**이 코드에서의 역할**
`UserRepository.findAllWithOrders()`에서 사용하여 N+1을 1번의 쿼리로 해결합니다.

**어떤 기능에 많이 활용되나요?**
- 주문 목록 조회 시 주문자 정보를 함께 표시
- 게시글 목록에서 작성자 정보를 함께 표시
- 상품 목록에서 카테고리 정보를 함께 표시

---

### @BatchSize

**정의**
연관 데이터를 개별 쿼리 대신 `IN` 절로 묶어 한 번에 조회하는 전략입니다. Fetch Join이 불가한 페이징 상황에서 대안으로 사용합니다.

**예제**
```java
@BatchSize(size = 100)
@OneToMany(mappedBy = "user")
List<Order> orders = new ArrayList<>();
```

```sql
-- N+1 (기존): 유저마다 개별 쿼리
SELECT * FROM orders WHERE user_id = 1
SELECT * FROM orders WHERE user_id = 2
...

-- @BatchSize(100) 적용 후: IN 절로 묶어서 조회
SELECT * FROM orders WHERE user_id IN (1, 2, 3, ..., 100)
```

**이 코드에서의 역할**
페이징이 필요한 목록 조회에서 Fetch Join 대신 쿼리 수를 줄이는 대안 전략으로 활용합니다.

**어떤 기능에 많이 활용되나요?**
- 페이지네이션이 있는 목록 API에서 연관 데이터를 효율적으로 조회
- 전역 설정으로 `spring.jpa.properties.hibernate.default_batch_fetch_size`를 지정해 일괄 적용

---

## 📝 정리

- **모든 연관관계는 기본적으로 `LAZY`로 설정한다.** `@ManyToOne`의 기본값은 EAGER이므로 반드시 명시해야 한다.
- **N+1은 LAZY 전략의 문제가 아니다.** "연관 데이터가 필요한데 명시적으로 함께 조회하지 않은 것"이 원인이다.
- **EAGER가 N+1의 해결책이 아니다.** JPQL에서는 EAGER 설정을 무시하고 N+1을 유발한다.
- **연관 데이터가 필요하면 Fetch Join을 명시적으로 추가한다.** `DISTINCT`도 함께 사용해 중복을 제거한다.
- **페이징 + 연관 데이터가 필요하면 `@BatchSize`를 사용한다.** Fetch Join은 페이징과 함께 사용할 수 없다.
