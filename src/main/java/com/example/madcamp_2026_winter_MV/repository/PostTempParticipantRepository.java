package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.PostTempParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostTempParticipantRepository extends JpaRepository<PostTempParticipant, Long> {

    List<PostTempParticipant> findByPost_PostId(Long postId);

    Optional<PostTempParticipant> findByPost_PostIdAndMember_MemberId(Long postId, Long memberId);

    boolean existsByPost_PostIdAndMember_MemberId(Long postId, Long memberId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostTempParticipant p WHERE p.post.postId = :postId")
    void deleteByPost_PostId(@Param("postId") Long postId);
}