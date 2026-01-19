package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.CategoryDto;
import com.example.madcamp_2026_winter_MV.entity.Category;
import com.example.madcamp_2026_winter_MV.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

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

    // 2.카테고리 생성 API (POST)
    @PostMapping
    public ResponseEntity<String> createCategory(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        // 중복 방지 로직 (이미 있는 이름이면 생성 안 함)
        if (categoryRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body("이미 존재하는 카테고리입니다.");
        }

        // 새 카테고리 저장
        Category category = Category.builder()
                .name(name)
                .icon("✨")
                .build();

        categoryRepository.save(category);

        return ResponseEntity.ok("카테고리가 생성되었습니다.");
    }

    //3. 카테고리 이름 중복 확인 API
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(@RequestParam String name) {
        boolean exists = categoryRepository.existsByName(name);
        return ResponseEntity.ok(Map.of("available", !exists));
    }
}