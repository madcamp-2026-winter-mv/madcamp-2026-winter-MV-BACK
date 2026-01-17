package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.entity.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 1. 특정 게시판(분반)의 게시글 목록 조회
    List<Post> findByRoom_RoomId(Long roomId);

    // 2. 내가 쓴 글 전체 개수 조회
    long countByMember(Member member);

    // 3. 내가 쓴 글 중 특정 타입(예: PARTY)의 개수 조회
    long countByTypeAndMember(PostType type, Member member);

    // 4. 내가 작성한 게시글 목록 조회
    List<Post> findByMember(Member member);

    // 5. 내가 댓글을 단 중복 없는 게시글 목록 조회
    List<Post> findDistinctPostsByComments_Member(Member member);

    // 6.  Category 엔티티의 categoryId를 참조
    List<Post> findByCategory_CategoryId(Long categoryId);

    // 7. 좋아요 수 기준 인기 게시글 조회
    @Query("SELECT p FROM Post p LEFT JOIN p.likes l GROUP BY p ORDER BY COUNT(l) DESC")
    List<Post> findHotPosts(Pageable pageable);

    // 8. 특정 타입의 게시글 중 제목에 특정 단어가 포함된 게시글 조회 (검색 기능용)
    List<Post> findByTypeAndTitleContaining(PostType type, String title);
}