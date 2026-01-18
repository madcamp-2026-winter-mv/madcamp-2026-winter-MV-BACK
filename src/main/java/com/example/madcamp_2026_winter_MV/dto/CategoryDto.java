package com.example.madcamp_2026_winter_MV.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

    @Getter
    @Builder
    @AllArgsConstructor
    public class CategoryDto {
        private Long categoryId;
        private String name;
        private String icon;
    }
