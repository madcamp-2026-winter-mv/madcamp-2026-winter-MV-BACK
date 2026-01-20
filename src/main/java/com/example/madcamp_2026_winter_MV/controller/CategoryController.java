package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.CategoryDto;
import com.example.madcamp_2026_winter_MV.entity.Category;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.repository.CategoryRepository;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    // 1. 카테고리 전체 목록 조회 API
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categories = categoryRepository.findAll().stream()
                .map(category -> CategoryDto.builder()
                        .categoryId(category.getCategoryId())
                        .name(category.getName())
                        .icon(category.getIcon())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    // 2. 카테고리 생성 API (POST)
    @PostMapping
    public ResponseEntity<String> createCategory(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (categoryRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body("이미 존재하는 카테고리입니다.");
        }

        Category category = Category.builder()
                .name(name)
                .icon("✨")
                .build();

        categoryRepository.save(category);

        return ResponseEntity.ok("카테고리가 생성되었습니다.");
    }

    // 3. 카테고리 이름 중복 확인 API
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(@RequestParam String name) {
        boolean exists = categoryRepository.existsByName(name);
        return ResponseEntity.ok(Map.of("available", !exists));
    }

    // 4. 카테고리 수정 API (PATCH) - 추가됨
    @PatchMapping("/{categoryId}")
    @Transactional
    public ResponseEntity<String> updateCategory(@PathVariable Long categoryId, @RequestBody Map<String, String> request) {
        // 5번 ID까지는 수정 불가 보호
        if (categoryId <= 5) {
            return ResponseEntity.badRequest().body("기본 카테고리는 수정할 수 없습니다.");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        String newName = request.get("name");
        if (newName != null && newName.equals(category.getName())) {
            return ResponseEntity.ok("카테고리 이름이 수정되었습니다.");
        }
        if (newName != null && categoryRepository.existsByName(newName)) {
            return ResponseEntity.badRequest().body("이미 존재하는 카테고리 이름입니다.");
        }

        if (newName != null) category.setName(newName);
        return ResponseEntity.ok("카테고리 이름이 수정되었습니다.");
    }

    // 5. 카테고리 삭제 API (DELETE) - 추가됨
    @DeleteMapping("/{categoryId}")
    @Transactional
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
        if (categoryId <= 5) {
            return ResponseEntity.badRequest().body("기본 카테고리는 삭제할 수 없습니다.");
        }

        Category targetCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        // 삭제 전 게시글 이전: 자유게시판(ID 1)으로 이동
        Category freeCategory = categoryRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("자유게시판 카테고리를 찾을 수 없습니다."));

        List<Post> posts = postRepository.findByCategory_CategoryId(categoryId);
        for (Post post : posts) {
            post.setCategory(freeCategory);
        }

        categoryRepository.delete(targetCategory);

        return ResponseEntity.ok("카테고리가 삭제되었으며, 기존 글들은 자유게시판으로 이동되었습니다.");
    }
}