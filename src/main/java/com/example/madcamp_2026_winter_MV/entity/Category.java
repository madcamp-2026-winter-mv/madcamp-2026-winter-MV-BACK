package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 게시판 종류 구분 엔티티
    @Column(nullable = false, length = 50)
    private String name;

    private String icon;
}