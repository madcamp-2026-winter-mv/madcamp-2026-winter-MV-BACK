package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.ChatMember;
import com.example.madcamp_2026_winter_MV.entity.ChatRoom;
import com.example.madcamp_2026_winter_MV.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);
    long countByChatRoom(ChatRoom chatRoom);
    List<ChatMember> findByMember(Member member);
}