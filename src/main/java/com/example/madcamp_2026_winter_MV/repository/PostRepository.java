package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 특정 게시판의 게시글만 가져오는 기능
    List<Post> findByRoom_RoomId(Long roomId);
}