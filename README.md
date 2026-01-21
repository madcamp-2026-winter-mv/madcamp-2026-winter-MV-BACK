#  몰 봐 (Madcamp View)

> **"몰입캠프에서 지금 뭘 봐야 할지 알려줌"** > Madcamp의 모든 소식과 활동을 한눈에 View하는 커뮤니티 플랫폼

---

## 1. 서비스 소개

몰 봐는 몰입캠프 참여자들을 위한 전용 커뮤니티 및 분반 관리 플랫폼입니다. 구글 로그인을 통해 간편하게 접속하며, 자유롭게 참가자들과 소통하며 자신이 속한 분반의 출석, 일정, 스크럼 발표자 정보를 실시간으로 확인할 수 있습니다.

* **Frontend:** [www.madcamp-view.com](https://www.madcamp-view.com)
* **Backend API:** [api.madcamp-view.com](https://api.madcamp-view.com)
* **API Documentation:** https://api.madcamp-view.com/swagger-ui/index.html#/

---

## 🛠 2. Tech Stack

### 1. Backend & Security

- **Language**: **Java 21**
- **Framework**: **Spring Boot 3.x**
- **Persistence**: **Spring Data JPA**
- **Security**: **Spring Security & OAuth 2.0** (Google Social Login)
- **Documentation**: **Swagger**

### 2. Database & Infrastructure

- **Database**: **MySQL 8.0**
- **Cloud Hosting**: **AWS EC2** (Ubuntu 22.04 LTS)
- **Network & Routing**:
    - **AWS Route 53**: 도메인 관리 및 DNS 라우팅
    - **AWS Application Load Balancer (ALB)**: **HTTPS(Port 443)** 리버스 프록시 및 SSL Termination 처리
    - **AWS Certificate Manager (ACM)**: SSL/TLS 인증서 발급 및 암호화 통신 관리
- **Containerization**: **Docker & Docker Compose**

### 3. CI/CD & DevOps

- **CI/CD Pipeline**: **GitHub Actions** (빌드 및 배포 자동화)
- **Image Registry**: **Amazon ECR (Elastic Container Registry)** (Docker 이미지 관리)
- **Automation Script**: GitHub Secrets를 활용한 민감 정보(DB ID/PW, API Key) 주입
  
### 4. Frontend
- **Framework** : Next.js 16, React 19
- **Language** : TypeScript 5
- **Styling** : Tailwind CSS 4, CVA, clsx, tailwind-merge
- **UI / Components** : Radix UI, shadcn/ui, Lucide React, Recharts, Embla Carousel, cmdk, Vaul, Sonner
- **Form & Validation** : React Hook Form, Zod, @hookform/resolvers
- **Real-time** : SockJS, @stomp/stompjs (STOMP)
- **Utils** : date-fns, next-themes
- **Analytics** : Vercel Analytics

---

## 3. 배포 아키텍처 (Deployment Architecture)

### 보안 및 통신 원리

1. **HTTPS (Port 443) 연결:** 가비아(Gabia) 도메인을 AWS Route 53에 위임하고, ACM을 통해 SSL/TLS 인증서를 적용하여 전 구간 암호화 통신을 구현
2. **SSL Termination:** ALB에서 HTTPS 요청을 복호화하여 내부 서버(Spring Boot)에는 8080 포트로 전달함으로써 서버 부하를 최적화
3. **CORS 정책:** 프론트엔드와 백엔드의 Origin 분리에 따른 보안 문제를 Spring Security 설정을 통해 해결

---

## 4. 핵심 기능 상세


<img width="1916" height="898" alt="스크린샷 2026-01-21 151246" src="https://github.com/user-attachments/assets/5e6fc32e-bab5-41a6-a332-d5bb8cd9d2ad" />

### 1. 홈 화면 (Dashboard)

<img width="1916" height="897" alt="스크린샷 2026-01-21 151229" src="https://github.com/user-attachments/assets/a3c22114-1698-47c5-82b0-ce0c20e1a39f" />

사용자가 속한 분반 데이터를 기반으로 개인마다 다르게 동적 렌더링됌
*구글로그인이후에 분반없이도 대시보드,카테고리를 볼순 있지만, 게시글 상세조회는 불가*

<img width="1913" height="899" alt="스크린샷 2026-01-21 151211" src="https://github.com/user-attachments/assets/76cb1579-e473-4160-bb8f-31ce8038ff38" />

- **분반 출석체크**
    - **출석 위젯:** 운영진이 출석을 활성화하면  출석 버튼 노출 (출석시간 내에 한번만 가능)
- **일정**
    - 운영진 등록한 일정을 시간순으로 나열.
    - 일정은 **남은 시간** 표시 (장소,준비물,시간등 표시)
- **스크럼 진행자**
    - 랜덤 알고리즘으로 선정된 발표자의 구글 프로필 이미지와 닉네임이 카드 형태로 노출.
    - 오늘의 스크럼 진행자는 [닉네임님]입니다!
- **실시간 HOT 3**
    - 전체 게시판 중 좋아요 5개 이상을 받은 게시글을 핫게시판으로 노출

---

### 2. 핵심 기능 상세 설계

### ① 분반(Room) & 관리 권한

- **입장:** 운영진이 랜덤 생성한 8자리 코드 입력 시 해당 분반 입장
- **운영진 권한:**
    - 관리자 페이지에서 멤버별 권한 수정 가능(분반관리)
    - 전용 대시보드: 출석 시작 버튼, 일정 등록 폼, 멤버 강퇴 버튼
 
      

### ② 스마트 출석 & 스크럼 진행자 선정

- **출석:** 운영진이 활성화한 시간 내에서 한번만 가능.
- **스크럼 발표자 선정:** 발표횟수가 낮은 사람들을 우선순위 큐에 담고, 그중에서 랜덤으로 추출 (당첨시 발표횟수 증가)

### ③ 커뮤니티
<img width="1914" height="904" alt="스크린샷 2026-01-21 151408" src="https://github.com/user-attachments/assets/6f63c4a5-3f47-46f1-84d2-325230b7b44b" />


- **카테고리:** 사용자가 자유롭게 카테고리를 생성 가능(DB 테이블에 카테고리 정의)
- **게시판** : 익명/닉네임중 골라 카테고리별로 게시글 작성 가능
- **팟(Party) 매칭:**
    - **닉네임 기반:** 신뢰가 중요한 '팟' 글에서는 댓글 작성자의 닉네임이 공개됨( 팟모집 게시판에서는 익명 불가)
    - **매칭 프로세스:** 글쓴이가 댓글 창에서 원하는 인원 체크 -> [채팅방 생성] 클릭 -> 채팅방 생성 ( 게시글 작성자는 강퇴 및 빈자리에 인원추가 가능 / 팟 개설시 참여자에게 알림 생성)
    - **채팅방** : 참여중인 채팅방 및 읽지않은 알림 갯수 표시
      <img width="1902" height="899" alt="스크린샷 2026-01-21 162321" src="https://github.com/user-attachments/assets/ec5b8388-a1b0-41e1-b1e8-08418e1dcf8c" />
      <img width="1918" height="904" alt="스크린샷 2026-01-21 162404" src="https://github.com/user-attachments/assets/cdcb2424-c08c-445e-a969-c04072263ce1" />
      <img width="1906" height="900" alt="스크린샷 2026-01-21 162835" src="https://github.com/user-attachments/assets/3a2a7a45-dea1-4169-9174-7ba756c0fcc5" />
      <img width="2539" height="1329" alt="KakaoTalk_20260121_162546102_01" src="https://github.com/user-attachments/assets/d72eb4c8-cfd8-4962-9169-a0f0ada6fe0b" />

<img width="1919" height="897" alt="스크린샷 2026-01-21 162429" src="https://github.com/user-attachments/assets/3a9618c6-0f80-4771-b20e-27fd190a0afc" />



- **검색:** 제목 또는 본문 키워드와 일치하는 게시글 검색 가능
    
<img width="1914" height="896" alt="스크린샷 2026-01-21 153955" src="https://github.com/user-attachments/assets/079fe125-7ffb-4f89-8dc7-b88edf77b56b" />
<img width="1915" height="895" alt="스크린샷 2026-01-21 154009" src="https://github.com/user-attachments/assets/068d7cff-582f-4f6d-a77c-afeb49868cb3" />

### ④ 익명 투표


- **참여 유도:** 투표하지 않은 유저는 결과 그래프를 볼 수 없게 처리 ( 한번만 투표가능)

---

### 3. 마이페이지& 인증

- **로그인:** Google OAuth 2.0.
- **프로필 정보:**
    - **이미지:** 구글 계정 이미지 그대로 가져옴.
    - **닉네임:** 가입 시 "몰입하는 [숫자]"로 자동 생성 -> 마이페이지에서 중복확인 후 수정 가능
    - **실명:** 분반안에서 관리 목적으로 구글 계정에서 가져오기
- **활동 관리:**
    - [내가 쓴 글], [댓글 단 글] , 페이징 조회.
    - [진행 중인 팟]: 현재 내가 포함된 채팅방 리스트로 바로가기.

<img width="1916" height="898" alt="스크린샷 2026-01-21 151432" src="https://github.com/user-attachments/assets/a2459dc3-9ccb-46f9-8c4e-e9b8a45b873b" />


- **시스템 설정:**
    - **알람(On/Off)**: DB에 알림 수신 동의 여부 저장 (off시 내 글에 댓글 달리거나, 팟 개설을 알수 없음)
 
  <img width="2536" height="1326" alt="KakaoTalk_20260121_162546102_02" src="https://github.com/user-attachments/assets/776350a5-02ca-4e76-98c6-bd29544375cd" />
    - **분반 탈퇴**: 탈퇴 시 모든 활동 내역은 유지되나 소속만 끊김.
    - **로그아웃**

    
  <img width="1918" height="911" alt="스크린샷 2026-01-21 151238" src="https://github.com/user-attachments/assets/0524881a-671c-4205-aff2-043bf23ba903" />
<img width="1913" height="899" alt="스크린샷 2026-01-21 151211" src="https://github.com/user-attachments/assets/3ce36def-1b20-4b4c-8302-3946f81af423" />

---


## 🗂 5. Database Schema (ERD)

| 분류 | 테이블명 | 설명 |
| --- | --- | --- |
| **그룹** | `Room` | 초대코드를 가진 독립된 분반(1~4분반) 단위. |
| **인증** | `Member` | 구글 이메일을 PK로 사용하는 유저 정보. 소속 Room 정보 포함. |
| **게시판** | `Category` | 자유/질문/정보 등 카테고리. (ID 1~5 시스템 보호) |
| **게시판** | `Post` | FREE, VOTE, PARTY 타입에 따른 게시글 본체. |
| **소통** | `Comment` | 게시글 하위 댓글. 익명 여부 포함. |
| **소통** | `Likes` | Member-Post 매핑을 통한 좋아요 중복 방지. |
| **투표** | `VoteOption` | 투표 게시글 내의 각 선택지 데이터. |
| **투표** | `VoteRecord` | 유저별 투표 참여 기록 (중복 방지). |
| **팟모집** | `PostTempParticipant` | 팟 확정 전 작성자가 찜한 임시 참가자 명단. |
| **채팅** | `ChatRoom` | 팟 확정 시 생성되는 Post 1:1 대응 채팅방. |
| **채팅** | `ChatMember` | 채팅방 참여 멤버 및 마지막 읽은 시간(`last_read_at`) 관리. |
| **채팅** | `ChatMessage` | 채팅방 내 실시간 메시지 데이터. |
| **알림** | `Notification` | 팟 확정, 댓글 등 실시간 알림 기록 및 읽음 관리. |
| **관리** | `Schedule` | 분반별 운영진 등록 공지 일정. |
| **관리** | `Attendance` | 유저별/날짜별 출석 체크 데이터. |

---

## ⚙️ 6. CI/CD Pipeline

GitHub Actions를 통해 코드 Push 시 자동으로 아래 과정이 수행됩니다.

1. **Build:** JDK 21 기반 프로젝트 빌드 (Gradle)
2. **Containerize:** Docker 이미지 생성 및 Amazon ECR Push
3. **Deploy:** SSH를 통해 EC2 접속 후 최신 이미지 Pull 및 컨테이너 교체 (Zero-downtime 지향)

---
