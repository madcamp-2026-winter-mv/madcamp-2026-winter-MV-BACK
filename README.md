# 몰봐 (Mol-Va) : Madcamp View

> 몰입캠프에서 지금 뭘 봐야 할지 알려줌 + Madcamp의 모든 것을 View
> 
---
## 배포 주소

---
## Tech Stack (Backend)

* **Framework:** Spring Boot 3.x
* **Language:** Java 17
* **Security:** Spring Security, OAuth 2.0 (Google)
* **Data:** Spring Data JPA, QueryDSL, MySQL 8.0, Redis (Caching)
* **Infrastructure:** AWS (EC2, S3, RDS), Docker
* **AI:** OpenAI API (GPT-4)
* **Communication:** WebSocket (STOMP)

---

## System Architecture

---

## Key Backend Features & Implementation

### 1. 분반 기반 멀티테넌시 권한 설계

* **초대 코드 시스템:** 6자리 난수 코드를 통한 분반 입장 및 그룹핑.
* **RBAC (Role-Based Access Control):** `OWNER`, `USER` 권한 계층을 설계하여 운영진 전용 기능(출석 시작, 일정 등록, 강퇴)에 대한 접근 제어.

### 2. 스마트 출석 및 가중치 랜덤 알고리즘

* **IP Filtering:** 캠퍼스 공인 IP 대역(CIDR) 비교를 통한 부정 출석 방지 로직.
* **발표자 선정 로직:** * 발표 횟수가 적은 사람에게 우선순위를 부여하는 **Weighted Random Selection** 구현
* 동시성 이슈를 방지하기 위한 트랜잭션 격리 수준 관리.


### 3. AI 기반 스크럼 자동화 및 데이터 전처리

* **OpenAI Prompt Engineering:** 유저의 단순 키워드 입력을 격식 있는 스크럼 일지로 변환하는 비동기(`@Async`) API 설계.
* **Vector Similarity (Planned):** 분실물 매칭을 위한 텍스트 임베딩 유사도 분석 로직 준비.

### 4. 고성능 커뮤니티 엔진

* **인기글(HOT) 선정 스케줄러:** Spring Scheduler를 활용하여 10분마다 좋아요 수(5개 이상)를 집계하고 `is_hot` 상태를 갱신하는 배치 작업.
* **QueryDSL 검색:** 복잡한 동적 쿼리(카테고리, 검색어, 기간 필터링)의 성능 최적화 및 Type-safe한 쿼리 작성.
* **팟(Party) 매칭:** N:M 관계의 참여자 관리 및 매칭 확정 시 실시간 FCM(Firebase Cloud Messaging) 알림 전송.

### 5. 실시간 인터랙션 (WebSocket)

* **Live Voting:** 투표 참여 시 전역 브로드캐스팅을 통해 결과 그래프를 실시간 갱신.
* **Real-time Notice:** 운영진의 긴급 공지나 출석 시작 알림을 지연 시간 없이 전달.

---

## Database ERD

---

## API Endpoints (Core)

| Category | Endpoint | Method | Description |


---

## Deployment & Infrastructure

---

## 개발 및 성과 (Expected Outcomes)
