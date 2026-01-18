package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.CategoryDto;
import com.example.madcamp_2026_winter_MV.entity.Category;
import com.example.madcamp_2026_winter_MV.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // 카테고리 전체 목록 조회 API
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


}