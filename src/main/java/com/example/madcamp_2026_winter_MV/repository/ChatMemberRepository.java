package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.ChatMember;
import com.example.madcamp_2026_winter_MV.entity.ChatRoom;
import com.example.madcamp_2026_winter_MV.entity.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    @Transactional
    void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);
    long countByChatRoom(ChatRoom chatRoom);

    @Query("SELECT cm FROM ChatMember cm JOIN FETCH cm.chatRoom WHERE cm.member = :member")
    List<ChatMember> findByMemberWithChatRoom(@Param("member") Member member);

    Optional<ChatMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    List<ChatMember> findByChatRoom(ChatRoom chatRoom);
}