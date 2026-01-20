package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 채팅방의 메시지를 시간순(오래된 순)으로 정렬해서 가져오기
    List<ChatMessage> findByChatRoom_ChatRoomIdOrderByTimestampAsc(Long chatRoomId);

    // 미읽음 메시지 수 카운트 쿼리
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom.chatRoomId = :roomId AND m.timestamp > :lastReadAt")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadAt") LocalDateTime lastReadAt);

}