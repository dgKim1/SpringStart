# 아키텍처 설계

## 디렉토리 구조

```
src
└── main
    ├── java
    │   └── com.example.demo
    │       ├── domain               # 📌 도메인별 패키지
    │       │   └── member
    │       │       ├── controller   # Member 관련 HTTP 요청 처리 (API 엔드포인트)
    │       │       ├── service      # Member 관련 비즈니스 로직 구현
    │       │       ├── dto          # Member 관련 데이터 전송 객체 (Request/Response)
    │       │       ├── entity       # Member 데이터베이스 테이블과 매핑되는 객체
    │       │       └── repository   # Member 데이터베이스 접근 로직 (JPA 인터페이스)
    │       │
    │       ├── common               # 📂 여러 도메인에서 공통으로 사용하는 코드
    │       │   └── util             # 유틸리티 클래스 (e.g., 날짜, 암호화 등)
    │       │
    │       └── global               # 🌍 애플리케이션 전역에 적용되는 코드
    │           └── config           # 각종 설정 클래스 (e.g., SecurityConfig)
    │
    └── resources
        ├── db/migration/            # DB 마이그레이션 스크립트 (Flyway, Liquibase)
        ├── application.yml          # 애플리케이션 주요 설정 파일
        └── static/                  # CSS, JS, 이미지 등 정적 리소스
```

---

## 패키지 설명

### `domain`

애플리케이션의 핵심 비즈니스 로직이 담기는 곳입니다. 회원(Member), 주문(Order), 상품(Product) 등 주요 기능(도메인) 단위로 패키지를 분리하여 코드를 관리합니다.
이렇게 하면 특정 기능과 관련된 코드를 쉽게 찾을 수 있고, 기능 간의 의존성을 명확하게 파악할 수 있습니다.

각 도메인 패키지는 아래 레이어로 구성됩니다.

| 레이어 | 역할 |
|--------|------|
| `controller` | HTTP 요청을 받아 서비스로 위임하고 응답을 반환 |
| `service` | 핵심 비즈니스 로직 구현 |
| `dto` | 요청/응답 데이터 전송 객체 (Request/Response) |
| `entity` | DB 테이블과 매핑되는 JPA 엔티티 |
| `repository` | DB 접근 인터페이스 (Spring Data JPA) |

### `common`

특정 도메인에 종속되지 않고, 여러 도메인에서 공통으로 사용되는 유틸리티성 코드를 모아두는 패키지입니다.

### `global`

애플리케이션의 전반적인 동작에 영향을 미치는 설정 코드를 모아두는 패키지입니다. 보안 설정, CORS 설정, 로깅 설정 등이 여기에 해당됩니다.
