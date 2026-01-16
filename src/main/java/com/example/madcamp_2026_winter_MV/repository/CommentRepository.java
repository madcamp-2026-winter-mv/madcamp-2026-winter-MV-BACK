package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostPostIdOrderByCommentIdAsc(Long postId);
}