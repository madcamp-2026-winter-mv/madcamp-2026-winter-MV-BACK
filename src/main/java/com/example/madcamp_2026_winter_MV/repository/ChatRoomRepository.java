package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 특정 게시글의 채팅방 존재 여부 확인 (중복 개설 방지용)
    boolean existsByPostId(Long postId);

    // 특정 게시글 ID로 채팅방 조회
    Optional<ChatRoom> findByPostId(Long postId);
}