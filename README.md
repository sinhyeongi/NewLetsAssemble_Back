# 🧩 New-LetsAssemble
### Scalable Group Matching Platform Backend

> 그룹 생성, 참가 신청, 승인/거절 관리 및
> 실시간 채팅 기능을 포함한 수평 확장 가능한 백엔드 서버

---

## 📌 Why this Project?

이 프로젝트는 단순 기능 구현이 아닌,

- 상태 전이(State Transition) 기반 도메인 설계
- 동시성 환경에서 인원 제한 불변성 보장
- 실시간 커뮤니케이션 처리
- 멀티 인스턴스 환경에서의 메시지 순서 보장
- 시간 의존 로직의 테스트 가능성 확보

'도메인 불변성과 분산 환경에서의 정합성 유지'를 중심으로 설계되었다.

---

##  🧱 Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Security (JWT)
- WebSocket + STOMP
- JPA (Hibernate)

### Data & Messaging
- MySQL
- Redis (Pub/Sub, INCR, Cache)

## Infra
- Docker
- Nginx
- AWS EC2 (Single AZ 기준)

---

## 🏛 Architecture

### Layered + Partial Hexagonal Architecture

- 핵심 비즈니스 로직은 Application/Domain 계층에 위치
- 외부 의존성은 Port/Adapter 구조로 분리
- 복잡도 증가를 고려해 Full Hexagonal 대신 Partial Hexagonal 구조 적용
- 프로젝트 복잡도와 학습 목적을 고려해 실용적인 수준의 아키텍처 적용

```
Presentation (REST / WebSocket)
↓
Application (UseCase + Port)
↓
Domain
↓
Infrastructure (JPA, Redis, JWT)
```
---

## 🧠 Core Domain Design

### 📍 주요 도메인

- **Party** : 그룹 정보 및 최대 인원 관리
- **PartyMember** : 그룹 소속 사용자
- **Role** : OWNER / MEMBER

---

### 🔄 PartyMember 상태 전이 설계

PartyMember는 다음과 같은 상태 흐름을 가집니다:
```
APPLIED → APPROVED
APPLIED → REJECTED
APPLIED → CANCELED
APPROVED → KICKED
```

#### 상태 설명

- **APPLIED** : 참가 신청 상태
- **APPROVED** : 승인되어 실제 그룹 인원으로 포함
- **REJECTED** : 그룹 관리자가 신청 거절
- **CANCELED** : 신청자가 직접 신청 취소
- **KICKED** : 승인 후 관리자가 강제 퇴장 처리

#### 설계 원칙

- 실제 그룹 인원 수는 `APPROVED` 상태만을 기준으로 계산
- 상태 변경은 트랜잭션 범위 내에서 수행
- 최대 인원 검증과 상태 전이를 동일 트랜잭션에서 처리
- 재신청 정책은 확장 가능하도록 설계

---

## 🎂 Age Restriction Design

파티 생성 시 최소 연령(minAge)을 설정할 수 있으며, 사용자는 해당 조건을 만족해야만 참가 신청 승인이 가능하다.

---

### 🧩 설계 의도

나이(age)는 시간에 따라 변하는 값이므로 DB에 저장하지 않고 `birthDate` 기반으로 동적 계산하도록 설계하였다.

#### ❌ age를 저장하지 않은 이유

- 매년 자동 갱신 필요
- 배치 처리 필요
- 데이터 정합성 문제 발생 가능
- 나이는 "속성"이 아닌 "계산 결과"

따라서 User 엔티티에는 `birthDate`만 저장한다.

---

### 🕒 Time Abstraction

시스템 시간을 직접 사용하지 않고 `Clock` 기반으로 추상화 하였다.

```java
public LocalDate today(){
   return LocalDate.now(clock);
}
```

- 테스트 시 고정 시간 주입 가능
- 시간 의존 로직의 예측 가능성 향상
- Asia/Seoul 기준 명확화

---

### 📜 윤년 출생자 정책

2월 29일 출생자의 경우 다음과 같이 처리한다:

- 윤년 → 2월 29일을 해당 연도 생일로 인정
- 평년 → 3월 1일을 해당 연도 생일로 처리

도메인 정책으로 분리하여 명확하게 정의하였다.

---

### 🔎 연령 검증 시점 설계

연령 제한은 두 단계에서 검증한다.

1️⃣ **참가 신청 시**
- 조건 미충족 신청을 사전 차단

2️⃣ **승인 시 재검증**
- 신청 이후 시간 경과 가능성 고려
- 승인 시점 기준 정합성 보장

> 승인 시점에 재검증함으로써 데이터 무결성을 강화하였다.

---

### 🧠 책임 분리

- 나이 계산 정책 → Domain Policy
- 연령 충족 여부 판단 → User 도메인
- 최소 연령 조건 보유 → Party 도메인
- 유스케이스 흐름 제어 → Application Layer

도메인 책임을 명확히 분리하여 응집도를 높였다.

---

### ⚠ 동시성 고려 사항

- 그룹 최대 인원 초과 방지를 위해 승인 로직을 `@Transactional` 범위 내 처리
- 동시 승인 요청 시 데이터 정합성 보장
- DB 검증 로직과 애플리케이션 검증 로직 병행 적용

---

## 💬 Realtime Chat (Sub Feature)

그룹 단위 실시간 채팅 기능 제공.

### ✔ 해결한 문제

- 멀티 인스턴스 환경에서 메시지 동기화
- 메시지 순서 보장
- Stateless 인증 기반 WebSocket 연결 처리

---

### 🔢 Message Ordering Strategy
Room(Party) 단위 메시지 순서 보장을 위해
Redis의 원자적 증가 연산(INCR)을 사용하였다.

```java
public long incrPartyLastSeq(long partyId){
        Long v = redis.opsForValue().increment(ChatRedisKeys.partyLastSeq(partyId));
        return v == null ? 0L : v;
    }
```

- Redis INCR은 원자적 연산
- 멀티 인스턴스 환경에서도 순서 충돌 없음
- DB 저장 시 seq 포함하여 정렬 기준으로 활용

---

### ⚖ Trade-offs

- Redis 의존성 증가
- 중앙 카운터 구조로 인해 고트래픽 환경에서는 병목 가능성 존재
- 트래픽 증가 시 분산 전략 도입 검토 필요


---

## 🔒 Transaction & Consistency Strategy

- 그룹 승인 로직은 `@Transactional` 범위 내 처리
- 데이터 정합성을 우선 보장
- 채팅 메시지는 DB commit 이후 Publish
- Publish 실패 대비 Outbox Pattern 확장 가능
- 승인 로직은 단순 조회 후 판단이 아닌, 상태 변경 시점에 재검증을 수행한다.

---

## 🔐 Security Design

- JWT 기반 Stateless 인증
- Access / Refresh Token → HttpOnly Cookie 저장
- 서버 간 세션 공유 불필요 (수평 확장 대응)
- WebSocket 연결 단계에서 인증 완료

---

## 🌐 Deployment Architecture

Docker Compose 기반 단일 EC2 배포

### 구성 요소
- Nginx
  - React build 정적 파일 서빙
  - `/api` Reverse Proxy
- Spring Boot(Backend)
- Redis
- MySQL

### 네트워크 흐름

Client
→ Nginx(80/443)
→ Backend(Internal Network)

Backend는 외부에 직접 노출하지 않고, Docker 내부 네트워크를 통해 Nginx에서만 접근 가능하도록 구성하였다.

---

## 🚀 How to Run

### Backend
```
./gradlew build
```
### Frontend
```
npm install
npm run build
```
### Docker 실행
```
docker-compose up --build
```

---

## 🚀 Scalability

### 현재 구조

- Stateless JWT 인증
- Redis 기반 메시지 순서 보장
- Pub/Sub 기반 인스턴스 동기화
- Single AZ 운영 가정

### 확장 가능 전략
- Redis Cluster 구성
- Muti AZ 배포
- 메시지 브로커(Kafka) 도입 가능
- Backend 다중 인스턴스 수평 확장

---

## 🧪 Testing Strategy

- UseCase 단위 테스트 수행
- 그룹 승인 동시성 테스트 검증
- 멀티 스레드 환경에서 Redis INCR의 원자성 보장 여부 검증
- 통합 테스트 기반 메시지 흐름 검증

---

## 📈 Future Improvements

- Outbox Pattern 도입을 통한 이벤트 유실 방지
- Redis 장애 대비 이중화 구성
- 부하 테스트 기반 병목 구간 분석

---

## 🎯 What I Learned

- 단순 기능 구현이 아닌, 도메인 불변성(invariant)을 지키는 설계의 중요성 이해
- 시간 의존 로직을 추상화하여 테스트 가능성을 높이는 설계 경험
- 분산 환경에서 순서 보장의 어려움과 해결 전략 학습

---

## 👨‍💻 Author

신현기
Backend Developer
