# JPA 엔티티 설계

## 핵심 어노테이션: `@Entity`와 `@Table`

### `@Entity`

이 클래스가 JPA가 관리해야 하는 엔티티임을 표시합니다. 필수 어노테이션입니다.

> **주의:** 기본 생성자(no-args constructor)가 반드시 필요하며, `final` 클래스에는 사용할 수 없습니다.

### `@Table(name = "...")`

엔티티 클래스가 매핑될 데이터베이스 테이블의 이름을 지정합니다. 생략하면 엔티티 클래스 이름을 테이블 이름으로 사용합니다. (예: `User` → `user`)

---

## 네이밍 컨벤션

테이블 및 컬럼 이름을 정할 때는 일관된 규칙을 따르는 것이 매우 중요합니다.

### 네이밍 전략 (Naming Strategy)

Java에서는 `camelCase`(예: `userName`)를, DB에서는 `snake_case`(예: `user_name`)를 사용하는 것이 일반적입니다.
Spring Boot는 이 변환을 자동으로 처리해주는 전략(`SpringPhysicalNamingStrategy`)을 기본으로 사용하므로, `@Column(name = "user_name")`처럼 명시하지 않아도 필드 `userName`은 자동으로 컬럼 `user_name`에 매핑됩니다.

### DB 예약어 주의

`USER`, `ORDER`, `GROUP` 등은 데이터베이스의 예약어일 수 있습니다. 테이블명이나 컬럼명으로 사용하면 예기치 않은 SQL 오류가 발생할 수 있으므로 무조건 피하는 것이 좋습니다.
부득이하게 사용해야 할 경우, `` @Table(name = "`order`") ``처럼 백틱으로 감싸서 이스케이프 처리를 할 수 있습니다.

---

## 기본 키(Primary Key) 매핑: `@Id`와 `@GeneratedValue`

### `@Id`

테이블의 기본 키(PK)에 해당하는 필드를 나타냅니다. 모든 엔티티는 `@Id`가 붙은 필드를 반드시 가져야 합니다.

### `@GeneratedValue(strategy = ...)`

기본 키의 값을 데이터베이스가 자동으로 생성하도록 위임하는 방법을 지정합니다.

| 전략 | 설명 |
|------|------|
| `GenerationType.IDENTITY` | PostgreSQL의 `GENERATED ALWAYS AS IDENTITY`처럼 DB가 직접 ID를 생성합니다. `INSERT` 이후에야 ID 값을 알 수 있습니다. |
| `GenerationType.SEQUENCE` | DB의 시퀀스 객체를 통해 ID를 할당받습니다. |
| `GenerationType.AUTO` | (기본값) DB 방언(Dialect)에 맞춰 JPA가 전략을 자동 선택합니다. |

---

## 필드-컬럼 매핑: `@Column`과 기타 어노테이션

### `@Column`

엔티티의 필드와 테이블의 컬럼을 매핑합니다. 다양한 속성으로 제약조건을 설정할 수 있습니다.

| 속성 | 설명 |
|------|------|
| `name` | 매핑할 컬럼 이름 지정 (예: `createdAt` → `created_at`) |
| `nullable = false` | `NOT NULL` 제약조건 설정 |
| `unique = true` | `UNIQUE` 제약조건 설정 |
| `length` | `VARCHAR` 타입 컬럼의 길이 지정 |

### `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate 기능)

데이터가 처음 생성될 때(`@CreationTimestamp`) 또는 수정될 때(`@UpdateTimestamp`)의 시간을 자동으로 기록해주는 편리한 기능입니다.

### `@DynamicInsert` / `@DynamicUpdate`

- **`@DynamicInsert`**: `INSERT` SQL 생성 시 값이 `null`이 아닌 필드만으로 쿼리를 만듭니다. DB에 설정된 `DEFAULT` 값을 적용하고 싶을 때 유용합니다.
- **`@DynamicUpdate`**: `UPDATE` SQL 생성 시 변경된 필드만으로 쿼리를 만듭니다. 불필요한 필드 업데이트를 막아 성능에 이점을 줄 수 있습니다.

---

## 예제 코드

```java
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column
    LocalDateTime updatedAt;

    @Builder
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
```
