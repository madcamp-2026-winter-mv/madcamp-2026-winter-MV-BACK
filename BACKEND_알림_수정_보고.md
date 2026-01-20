# Backend 알림 기능 수정/추가 보고

## 1. 신규 추가

### 1.1 `NotificationType` enum
- **파일**: `entity/NotificationType.java`
- **내용**: `COMMENT`(내 글 댓글), `CHAT_INVITE`(채팅방 초대)

### 1.2 `Notification` 엔티티 – `type` 컬럼
- **파일**: `entity/Notification.java`
- **변경**: `@Enumerated(EnumType.STRING) @Column(name = "type")` 필드 추가
- **기존 DB**: `type` null 허용. DTO에서 null → `"COMMENT"`로 매핑
- **주의**: `type` 컬럼 추가 시 기존 DB는 마이그레이션 또는 `ddl-auto=update`로 반영 필요

### 1.3 `NotificationRepository` – 사이드바용 카운트
- **파일**: `repository/NotificationRepository.java`
- **메서드**: `countByReceiverAndIsReadFalseAndTypeIn(Member, List<NotificationType>)`
- **용도**: COMMENT, CHAT_INVITE만 미읽음 개수 조회 (알람3 채팅 미읽음 제외)

### 1.4 `NotificationController` – API 추가
- **파일**: `controller/NotificationController.java`
- **추가 API**
  - `PATCH /api/notifications/{notificationId}/read`  
    - 알림 개별 읽음 처리 (이동 없음)
  - `GET /api/notifications/unread-count/sidebar`  
    - 사이드바 배지: COMMENT·CHAT_INVITE 미읽음 개수 (숫자만, 0이면 미표시)

---

## 2. 수정 사항

### 2.1 `NotificationResponseDto`
- **파일**: `dto/NotificationResponseDto.java`
- **추가 필드**: `type` (String, `"COMMENT"` | `"CHAT_INVITE"`)
- **매핑**: `type == null` → `"COMMENT"` (기존 데이터 호환)

### 2.2 `NotificationService`
- **파일**: `service/NotificationService.java`
  - **`createNotification(Member, Post, String)` (COMMENT)**
    - `type = COMMENT`, `url = "/community/" + post.getPostId()` 로 변경  
      (기존 `"/posts/" + post.getPostId()`에서 변경)
  - **`createNotificationForChatInvite(Member, String content, String url)` (신규)**
    - CHAT_INVITE 알림 생성·저장 및 SSE 전송
  - **`markAsRead(Long notificationId)` (신규)**
    - 읽음 처리만 수행 (이동 URL 없음)
  - **`getUnreadCountForSidebar(Member)` (신규)**
    - COMMENT, CHAT_INVITE만 미읽음 개수 반환

### 2.3 `CommentService`
- **변경 없음**  
- `createNotification(receiver, post, content)` 그대로 사용  
  → 내부에서 `type=COMMENT`, `url=/community/{postId}` 적용  
- **유지**: 본인 댓글·allowAlarm 미수령자에게는 알림 미발송

### 2.4 `PartyService`
- **파일**: `service/PartyService.java`
  1. **`confirmAndCreateChat` (팟 확정)**
     - `createNotification` → `createNotificationForChatInvite` 로 변경
     - 내용: `'"{post.title}" 팟의 최종 멤버로 확정되었습니다. 채팅방에 참여해보세요!'`
     - URL: `/chat?room={chatRoomId}`
  2. **`addMemberToParty` (멤버 추가 초대)**
     - `createNotificationForChatInvite` 호출 추가
     - 내용: `'"{post.title}" 채팅방에 초대되었습니다.'`
     - URL: `/chat?room={chatRoomId}`
     - `newMember.isAllowAlarm()` 일 때만 발송

---

## 3. 알림이 생성·표시되는 경우 (정리)

| 알람 | 유형        | 발생 시점                         | 비고                    |
|------|-------------|-----------------------------------|-------------------------|
| 알람1 | COMMENT     | 내 글에 **타인** 댓글             | `allowAlarm` 적용       |
| 알람2 | CHAT_INVITE | 팟 확정 시 최종 멤버 선정         | `allowAlarm` 적용       |
| 알람2 | CHAT_INVITE | 기존 채팅방에 멤버 추가로 초대    | `allowAlarm` 적용       |

## 4. 알림을 만들지 않는 것 (기존·요구사항 유지)

- 커뮤니티 알람 (별도 타입 없음)
- 일정 알람
- **내 글에 내가 단 댓글** (CommentService에서 작성자와 동일인 제외)
- **내 글 좋아요** (Like 알림 미구현)

---

## 5. DB 마이그레이션

- `notification` 테이블에 `type VARCHAR(20) NULL` 컬럼 추가
- `ddl-auto=update` 사용 시 기존 스키마에 자동 반영 가능  
- 기존 행의 `type`은 null → DTO/로직에서 `"COMMENT"`로 간주

---

## 6. 배포·테스트 시 확인 사항

1. **알림 생성**
   - 타인이 내 글에 댓글 → COMMENT, `/community/{postId}` 링크
   - 팟 확정·채팅방 멤버 추가 → CHAT_INVITE, `/chat?room={chatRoomId}` 링크
2. **API**
   - `PATCH /api/notifications/{id}/read` → 200, 해당 알림만 `isRead=true`
   - `GET /api/notifications/unread-count/sidebar` → COMMENT·CHAT_INVITE 미읽음 개수
3. **채팅 미읽음(알람3)**  
   - `ChatRoomResponseDto.unreadCount`, `POST /api/party/rooms/{id}/read` 등 기존 채팅 로직 유지  
   - 사이드바 배지는 COMMENT·CHAT_INVITE만 사용

---

## 7. 추가 수정 (채팅 프로필·게시글 참가자 노출)

### 7.1 `ChatMessageDto`
- **파일**: `dto/ChatMessageDto.java`
- **추가 필드**: `senderProfileImageUrl` (String, nullable) — 채팅방 멤버 기준 닉네임 매칭으로 설정

### 7.2 `ChatMemberRepository`
- **메서드**: `List<ChatMember> findByChatRoom(ChatRoom chatRoom)`

### 7.3 `PartyService`
- **`getChatMessages`**: 채팅방 멤버의 닉네임→프로필이미지 맵을 만들어 각 메시지에 `senderProfileImageUrl` 세팅
- **`findSenderProfileImageUrl(Long chatRoomId, String senderNickname)` (신규)**: 실시간 브로드캐스트 전 발신자 프로필 조회

### 7.4 `ChatController`
- **`sendMessage`**: `saveMessage` 후 `findSenderProfileImageUrl`로 `senderProfileImageUrl` 세팅 후 `convertAndSend`

### 7.5 `PostResponseDto`
- **추가 필드**: `chatRoomId` (Long, null 가능), `isChatParticipant` (boolean)
- **용도**: 팟 모집 완료 글에서 ‘채팅방에서 대화를 나눠보세요 / 채팅방으로 이동’ 블록을 **참가자에게만** 노출

### 7.6 `PostService`
- **의존성**: `ChatMemberRepository` 추가
- **`getPostDetail`**: `post.type == PARTY` 일 때 `chatRoomRepository.findByPostId` 로 채팅방 조회 후  
  `chatRoomId` 설정, `chatMemberRepository.existsByChatRoomAndMember(room, member)` 로 `isChatParticipant` 설정
