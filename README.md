# 🏗 New-LetsAssemble
### Scalable Realtime Chat Architecture with Redis Pub/Sub

> 다중 인스턴스 환경에서 메시지 일관성과 순서를 보장하도록 설계한
> WebSocket 기반 실시간 채팅 서버

---

## 📌 Why this Project?

단순 CRUD 기반 채팅 서버가 아닌, **수평 확장이 가능한 실시간 메시징 구조 설계**를 목표로 했습니다.

특히 다음 문제를 해결하는 데 집중했습니다:

- 멀티 인스턴스 환경에서 메시지 동기화
- 동시성 환경에서 메시지 순서 보장
- Stateless 인증 기반 WebSocket 연결 처리

---

## 🧱 Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Security (JWT)
- WebSocket + STOMP
- JPA (Hibernate)

### Data & Messaging
- MySQL
- Redis (Pub/Sub, INCR Sequence, Cache)

### Infra
- Docker
- AWS (EC2 기반 배포 예정)

---

## 🏛 Architecture

### Layered + Partial Hexagonal Architecture

- 핵심 비즈니스 로직은 Application/Domain에 위치시키되, 복잡도 증가를 방지하기 위해 모든 외부 의존성을 완전히 분리하지는 않은 Partial Hexagonal 구조 적용

```
Client
   ↓
WebSocket / REST (Presentation)
   ↓
Application (UseCase + Port)
   ↓
Domain
   ↓
Infrastructure (JPA, Redis, JWT)
```

### ✔ 설계 의도

- Application 계층에서 **Port 인터페이스 정의**
- Infrastructure 계층에서 **Adapter 구현**
- Redis, DB, JWT 구현체 교체 가능성 고려
- 의존성 역전(Dependency Inversion) 적용

---

## 💬  Realtime Message Flow

1.  WebSocket Handshake 단계에서 JWT 검증
2.  Room 단위 Redis `INCR`로 Sequence 발급
3.  DB에 메시지 저장 (영속성 보장)
4.  Redis Pub/Sub 발행
5.  각 서버 인스턴스에서 구독 후 클라이언트 브로드캐스트

---

## 🔢 Message Ordering Strategy

### ✔ 문제
멀티 인스턴스 환경에서 메시지 순서가 어긋날 수 있음

### ✔ 해결
- Room 단위 Redis `INCR` 사용
- 원자적 증가 연산으로 동시성 제어
- DB 저장 시 seq 포함

### 🔎 Trade-off
- Redis 의존성 증가
- 중앙 카운터 구조로 인해 고트래픽 환경에서는 병목 가능성 존재
- 트래픽 증가 시 분산 전략 도입 검토 필요

---

## 🔒 Transaction Boundary

- DB 저장은 `@Transactional` 범위 내에서 수행
- 데이터 영속성을 우선 보장
- Publish는 DB commit 이후 실행하여 데이터 정합성을 우선 보장
- Publish 실패 시 재시도 또는 Outbox 패턴 확장 가능

---

## 🔄 Failure Handling Strategy

### Case 1. DB 저장 실패
→ Publish 진행하지 않음
### Case 2. Publish 실패
→ 재시도 전략 확장 가능
→ Outbox Pattern 도입 가능

---

## 🔐 Security Design

- JWT 기반 Stateless 인증
- Access / Refresh Token → HttpOnly Cookie 저장
- WebSocket 연결 단계에서 인증 완료
- 메시지 단위 인증 비용 제거
- 서버 간 세션 공유 불필요 (수평 확장 대응)

---

## 🚀 Scalability

- WebSocket 서버 수평 확장 시 Redis Pub/Sub을 통해 인스턴스 간 메시지 동기화
- JWT 기반 Stateless 인증으로 서버 간 세션 공유 없이 확장 가능
- Redis Cluster 구성을 통해 캐시 및 메시징 레이어 확장 가능
- 메시지 브로커 추상화를 고려한 구조 설계 (향후 Kafka 등으로 확장 가능)

---

## 🧪 Testing Strategy

- UseCase 단위 테스트 수행
- 멀티 스레드 환경에서 Redis INCR의 원자성 보장 여부 검증
- 통합 테스트 기반 메시지 흐름 검증
- 향후 부하 테스트 도입 예정

---

## 📈 Future Improvements

- Outbox Pattern 도입
- Redis 장애 대비 이중화 구성
- 부하 테스트 기반 병목 구간 분석

---

## 🎯 What I Learned

- 실시간 메시징 시스템에서의 순서 보장 전략
- Pub/Sub 기반 서버 간 동기화 구조
- 인증과 WebSocket의 결합 설계
- 트랜잭션 경계 설정의 중요성
- 확장성을 고려한 구조 설계 경험

---

## 👨‍💻 Author

신현기
Backend Developer
