# 서비스 레이어 데이터 흐름

## 데이터 변환 흐름

"사용자 생성" API를 예로 들면 데이터는 다음과 같이 각 객체로 변환되며 흐릅니다.

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

## 단계별 설명

| 단계 | 방향 | 객체 | 역할 |
|------|------|------|------|
| 1 | Client → Controller | `UserCreateRequest` | 클라이언트가 보낸 JSON을 객체로 받아 유효성 검증 |
| 2 | Controller → Service | `UserCreateDto` | Request를 Service가 사용하기 편한 형태로 변환하여 전달 |
| 3 | Service → Repository | `User` Entity | Dto를 기반으로 Entity를 생성하여 저장 요청 |
| 4 | Repository → DB | `User` Entity | Entity를 데이터베이스에 저장 |
| 5 | Service → Controller | `UserResponse` | 저장된 Entity를 응답에 필요한 데이터만 담아 반환 |
| 6 | Controller → Client | JSON | Response 객체를 JSON으로 변환하여 최종 응답 |

## 핵심 원칙

- **Controller**: `Request` / `Response` 객체만 다루고, Entity를 직접 반환하지 않는다.
- **Service**: 비즈니스 로직의 중심. `Dto`를 받아 `Entity`를 생성·조작하고 `Response`로 변환하여 반환한다.
- **Repository**: `Entity`만 다룬다. DB 접근 역할만 수행한다.
- **Entity를 외부에 노출하지 않는다**: API 응답에 Entity를 직접 사용하면 DB 구조가 외부에 노출되고 순환 참조 문제가 발생할 수 있다.
