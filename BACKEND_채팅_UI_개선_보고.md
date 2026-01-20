# 백엔드 수정 보고: 채팅 시스템 메시지·채팅방 이미지·스크럼 진행자 API

## 개요
프론트 요구사항(채팅 탈퇴/강퇴 시스템 메시지, 몰입캠프 UI, 시스템 메시지 DB 저장, 채팅방 대표 이미지, 스크럼 진행자 프로필) 반영을 위해 백엔드 DTO·서비스·컨트롤러를 수정·추가했습니다.

---

## 1. DTO

### 1.1 `LeaveOrKickResult` (신규)
- **경로**: `dto/LeaveOrKickResult.java`
- **용도**: `PartyService.leaveParty` 반환값. 탈퇴/강퇴 구분.
- **필드**:
  - `targetNickname` (String): 퇴장/강퇴된 멤버 닉네임
  - `kicked` (boolean): `true` = 방장이 강퇴, `false` = 본인 나가기

### 1.2 `ChatRoomResponseDto` (수정)
- **추가 필드**:
  - `creatorProfileImageUrl` (String): 채팅방 개설자(글쓴이/방장) 프로필 이미지 URL. 채팅방 대표 이미지용.

### 1.3 `ChatMessageDto`
- 기존 필드 활용. 시스템 메시지 시 `senderNickname="몰입캠프"`, `senderProfileImageUrl="/madcamp_logo.png"` 설정.

---

## 2. Service

### 2.1 `PartyService`

#### `saveSystemMessage(chatRoomId, senderNickname, content)` (신규)
- 시스템(몰입캠프) 공지 메시지를 `ChatMessage`로 DB 저장.
- 입·퇴장·강퇴 등 시스템 메시지가 새로고침 후에도 유지되도록 사용.

#### `leaveParty` (수정)
- **반환**: `String` → `LeaveOrKickResult`
- 서비스 내에서 `kicked = isOwner && !isSelf` 로 구분 후 `LeaveOrKickResult(target.getNickname(), kicked)` 반환.

#### `getChatMessages` (수정)
- `senderNickname`이 `"몰입캠프"`인 경우 `senderProfileImageUrl`을 `"/madcamp_logo.png"`로 세팅.

#### `getMyChatRooms` (수정)
- `Post`에서 `post.getMember().getProfileImage()`로 개설자 프로필 URL 조회.
- `ChatRoomResponseDto.creatorProfileImageUrl`에 매핑.

---

## 3. Controller

### 3.1 `PartyController`

#### `leaveOrKickMember` (수정)
- `leaveParty` 반환값 `LeaveOrKickResult` 사용.
- 문구 분리:
  - `kicked == true` → `"{닉네임}님이 강퇴당했습니다."`
  - `kicked == false` → `"{닉네임}님이 채팅방을 나갔습니다."`
- `partyService.saveSystemMessage(chatRoomId, "몰입캠프", content)` 호출로 DB 저장.
- WebSocket 전송 메시지:
  - `senderNickname="몰입캠프"`
  - `senderProfileImageUrl="/madcamp_logo.png"`
  - `timestamp=LocalDateTime.now().toString()`

#### `addMember` (수정)
- `addMemberToParty` 성공 후 `saveSystemMessage(chatRoomId, "몰입캠프", "{닉네임}님이 들어왔습니다!")` 호출.
- WebSocket 전송 시 `senderNickname="몰입캠프"`, `senderProfileImageUrl="/madcamp_logo.png"`, `timestamp` 설정.

### 3.2 `RoomController`

#### `getCurrentPresenter` (수정)
- 응답 Map에 `presenterProfileImageUrl` 추가.
- `presenter.getProfileImage()` 사용, null일 경우 `""` 반환.

---

## 4. Entity / Repository
- 변경 없음. `ChatMessage`에 `senderNickname="몰입캠프"`로 저장.

---

## 5. 정리

| 구분 | 파일 | 내용 |
|------|------|------|
| DTO 신규 | `dto/LeaveOrKickResult.java` | 탈퇴/강퇴 결과 |
| DTO 수정 | `dto/ChatRoomResponseDto.java` | `creatorProfileImageUrl` |
| Service | `PartyService.java` | `saveSystemMessage`, `leaveParty` 반환 타입 및 퇴장 문구 분리, `getChatMessages` 몰입캠프 프로필, `getMyChatRooms` 개설자 이미지 |
| Controller | `PartyController.java` | `leaveOrKickMember`·`addMember` 시스템 메시지 DB 저장 및 몰입캠프 WebSocket DTO |
| Controller | `RoomController.java` | `getCurrentPresenter`에 `presenterProfileImageUrl` |

---

## 6. [추가] 채팅방-게시글 오류 수정

### 6.1 모집 완료 글 삭제 가능 (`PostService.deletePost`)
- **문제**: 모집 완료 후 생성된 `ChatRoom`이 `post_id`(UNIQUE)로 `Post`를 참조해, 글 삭제 시 "데이터 중복 또는 제약 조건 위반" 발생.
- **수정**: `deletePost`에서 `postRepository.delete(post)` **전에** `chatRoomRepository.findByPostId(postId).ifPresent(chatRoomRepository::delete)` 호출.
- `ChatRoom` 삭제 시 `ChatMember`, `ChatMessage`는 `cascade=ALL`로 함께 삭제됨.

### 6.2 탈퇴/강퇴 시 모집완료 유지 (`PartyService.leaveParty`)
- **문제**: 채팅방에서 멤버 탈퇴/강퇴 시 `currentParticipants < maxParticipants`인 경우 `post.setClosed(false)`로 돌려, 모집완료 글이 다시 "모집중"으로 바뀜.
- **수정**: `if (post.getCurrentParticipants() < post.getMaxParticipants()) { post.setClosed(false); }` 블록 **제거**.
- 채팅방이 한 번 생성된 글은 인원이 줄어도 **모집 완료 상태 유지**. 같은 글로 채팅방이 다시 생성되지는 않음 (`existsByPostId` 체크 유지).
