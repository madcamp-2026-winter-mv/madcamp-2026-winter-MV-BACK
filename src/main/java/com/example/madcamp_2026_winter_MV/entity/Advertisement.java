package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long id;

    private String imageUrl;  // 업로드된 이미지 경로
    private String adName;    // 관리용 광고 이름
    private int displayOrder; // 노출 순서 (1번 칸, 2번 칸 구분용)
}