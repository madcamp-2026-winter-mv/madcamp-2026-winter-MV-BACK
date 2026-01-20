# Backend 수정 보고서 — 일정 삭제, 채팅방 삭제, 채팅방 멤버 목록/강퇴

## 1. 일정 삭제 (관리자 페이지)

### 1.1 RoomService

**파일:** `src/main/java/com/example/madcamp_2026_winter_MV/service/RoomService.java`

- **추가 메서드:** `deleteSchedule(Long roomId, Long scheduleId)`
  - `scheduleId`로 `Schedule` 조회, 없으면 `IllegalArgumentException`
  - 해당 일정의 `roomId`가 요청 `roomId`와 일치하는지 검사
  - 일치하지 않으면 `IllegalArgumentException("해당 분반의 일정이 아닙니다.")`
  - `scheduleRepository.delete(schedule)` 수행

### 1.2 AdminController

**파일:** `src/main/java/com/example/madcamp_2026_winter_MV/controller/AdminController.java`

- **추가 API:**
  - `DELETE /api/admin/rooms/{roomId}/schedules/{scheduleId}`
  - `deleteSchedule(roomId, scheduleId)` 호출
  - `ResponseEntity.noContent().build()` (204) 반환
  - 기존 `addSchedule`와 동일하게 `@AuthenticationPrincipal OAuth2User` 사용 (인증/관리자 권한은 `SecurityConfig` 등에서 처리 가정)

---

## 2. 채팅방 멤버 목록 (채팅방 유저 확인, 강퇴/나가기 전제)

### 2.1 DTO

**신규 파일:** `src/main/java/com/example/madcamp_2026_winter_MV/dto/ChatMemberResponseDto.java`

| 필드         | 타입    | 설명                                    |
|--------------|---------|-----------------------------------------|
| memberId     | Long    | 멤버 ID                                 |
| nickname     | String  | 닉네임                                  |
| profileImage | String  | 프로필 이미지 URL (nullable)             |
| isOwner      | boolean | 이 채팅방(팟)의 방장(게시글 작성자) 여부 |

### 2.2 PartyService

**파일:** `src/main/java/com/example/madcamp_2026_winter_MV/service/PartyService.java`

- **추가 메서드:** `getChatRoomMembers(Long chatRoomId, String email)`
  - `chatRoomId`로 `ChatRoom` 조회
  - `email`로 `Member` 조회
  - `chatMemberRepository.existsByChatRoomAndMember(room, requester)`로 **요청자가 해당 채팅방 참가자인지** 확인
    - 참가자가 아니면 `IllegalStateException("참여 중인 채팅방만 멤버 목록을 조회할 수 있습니다.")`
  - `room.getPostId()`로 `Post` 조회 후 `post.getMember()`를 방장(owner)으로 사용
  - `chatMemberRepository.findByChatRoom(room)` 결과를 `ChatMemberResponseDto` 리스트로 변환
    - 각 `ChatMember`의 `member`가 방장이면 `isOwner=true`, 아니면 `false`

### 2.3 PartyController

**파일:** `src/main/java/com/example/madcamp_2026_winter_MV/controller/PartyController.java`

- **추가 API:**
  - `GET /api/party/rooms/{chatRoomId}/members`
  - `partyService.getChatRoomMembers(chatRoomId, email)` 호출
  - `ResponseEntity.ok(members)`로 `List<ChatMemberResponseDto>` 반환
  - `principal == null`이면 `401` 반환

---

## 3. 채팅방 삭제 / 멤버 퇴장(강퇴·나가기)

다음 API는 **기존 구현**이며, 이번에 백엔드에서는 변경하지 않았습니다.  
프론트엔드에서 연동하여 사용합니다.

- **채팅방 삭제 (방장만)**  
  - `DELETE /api/party/rooms/{chatRoomId}`  
  - `PartyController.deleteChatRoom`  
  - `PartyService.deleteChatRoom(chatRoomId, email)` — 방장 여부 검사 후 삭제

- **멤버 퇴장 (나가기·강퇴)**  
  - `DELETE /api/party/rooms/{chatRoomId}/members/{memberId}`  
  - `PartyController.leaveOrKickMember`  
  - `PartyService.leaveParty(chatRoomId, memberId, requestUserEmail)`  
    - **방장:** 그 채팅방의 다른 멤버를 강퇴 가능  
    - **일반 멤버:** 본인(`memberId == requestUser.getMemberId()`)만 나가기 가능  
    - 그 외: `IllegalStateException("권한이 없습니다.")`

---

## 4. DTO 사용 요약

- **일정 삭제:**  
  - 요청: path variable `roomId`, `scheduleId`  
  - 응답: 204 No Content (Body 없음)  
  - 별도 DTO 미사용

- **채팅방 멤버 목록:**  
  - 요청: path variable `chatRoomId`  
  - 응답: `List<ChatMemberResponseDto>`

- **채팅방 삭제 / 멤버 퇴장:**  
  - 기존 `PartyController`/`PartyService` 구현 그대로 사용  
  - 별도 DTO 미사용 (path variable 및 `ChatMessageDto` 등 기존 DTO만 사용)

---

## 5. 수정/추가 파일 목록

| 구분   | 경로 |
|--------|------|
| 신규   | `dto/ChatMemberResponseDto.java` |
| 수정   | `service/RoomService.java` (`deleteSchedule` 추가) |
| 수정   | `controller/AdminController.java` (일정 삭제 API 추가) |
| 수정   | `service/PartyService.java` (`getChatRoomMembers` 추가) |
| 수정   | `controller/PartyController.java` (채팅방 멤버 목록 API 추가) |

---

## 6. API 목록 (이번 작업 관련)

| Method | URL | 설명 |
|--------|-----|------|
| DELETE | `/api/admin/rooms/{roomId}/schedules/{scheduleId}` | 일정 삭제 (관리자) |
| GET    | `/api/party/rooms/{chatRoomId}/members` | 채팅방 멤버 목록 (참가자만, `isOwner` 포함) |
| DELETE | `/api/party/rooms/{chatRoomId}` | 채팅방 삭제 (방장만, 기존) |
| DELETE | `/api/party/rooms/{chatRoomId}/members/{memberId}` | 멤버 퇴장/강퇴 (기존) |
