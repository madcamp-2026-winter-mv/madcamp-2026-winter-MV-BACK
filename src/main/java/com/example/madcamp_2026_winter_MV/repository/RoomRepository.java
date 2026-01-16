package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // 초대 코드로 분반 찾기 - 입장 기능
    Optional<Room> findByInviteCode(String inviteCode);
}