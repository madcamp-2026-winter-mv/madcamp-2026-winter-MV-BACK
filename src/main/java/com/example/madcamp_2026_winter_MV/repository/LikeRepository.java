package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Like;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    Optional<Like> findByMemberAndPost(Member member, Post post);

    // 특정 게시글의 좋아요 개수 조회
    long countByPost(Post post);
}