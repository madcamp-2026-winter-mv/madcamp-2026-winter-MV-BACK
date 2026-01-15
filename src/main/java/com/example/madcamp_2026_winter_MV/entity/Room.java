package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String inviteCode; // 예: MAD012

    @Column(nullable = false)
    private String name; // 분반 명칭 (2026-몰입캠프-겨울학기-1분반)

    private String notice; // 공지사항

    @Builder.Default
    private boolean isAttendanceActive = false; // 출석 활성화 여부

    private Long currentPresenterId; // 오늘의 제물(발표자) Member ID

    // Room에 속한 멤버 리스트
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Member> members = new ArrayList<>();
}