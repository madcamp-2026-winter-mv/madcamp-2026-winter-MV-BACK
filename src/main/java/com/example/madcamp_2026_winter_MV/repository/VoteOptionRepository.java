package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    // 특정 게시글(투표글)에 속한 모든 선택지 목록
    List<VoteOption> findByPostId(Long postId);
}