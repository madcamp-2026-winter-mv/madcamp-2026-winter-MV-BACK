package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 1. 기존 메서드: 특정 게시글의 댓글 목록 조회
    List<Comment> findByPostPostIdOrderByCommentIdAsc(Long postId);

    // 2. 사용자가 댓글을 남긴 서로 다른 게시글의 총 개수 세기
    @Query("SELECT COUNT(DISTINCT c.post) FROM Comment c WHERE c.member = :member")
    long countDistinctPostByMember(@Param("member") Member member);

    // 3. 내가 댓글을 단 목록 조회
    List<Comment> findByMember(Member member);

    boolean existsByPost_PostIdAndMember_MemberId(Long postId, Long memberId);
}