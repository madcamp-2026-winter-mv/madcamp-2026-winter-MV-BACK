package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 1. 이메일로 가입 여부 확인
    Optional<Member> findByEmail(String email);

    // 2. 닉네임 중복 여부 확인 (마이페이지 닉네임 수정 시 사용)
    boolean existsByNickname(String nickname);
}