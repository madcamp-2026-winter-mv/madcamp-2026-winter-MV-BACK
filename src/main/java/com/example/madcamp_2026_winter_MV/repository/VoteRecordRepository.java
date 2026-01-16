package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {

    // 특정 사용자가 특정 게시글(투표)에 이미 참여했는지 확인
    // 이 결과가 true이면 UI에서 투표 결과를 보여주고, false이면 결과 숨기기
    boolean existsByMemberIdAndPostId(Long memberId, Long postId);
}