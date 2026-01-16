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
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(nullable = false, unique = true, length = 10)
    private String inviteCode; // 예: MAD012

    @Column(nullable = false, length = 100)
    private String name; // 분반 명칭

    @Column(columnDefinition = "TEXT")
    private String notice; // 공지사항

    @Builder.Default
    private boolean isAttendanceActive = false; // 출석 활성화 여부

    private Long currentPresenterId; // 오늘의 발표자 Member ID

    // Room에 속한 멤버 리스트
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Member> members = new ArrayList<>();

    public void addMember(Member member) {
        this.members.add(member);
        if (member.getRoom() != this) {
            member.setRoom(this);
        }
    }

    // 공지사항 업데이트 메서드
    public void updateNotice(String notice) {
        this.notice = notice;
    }

    // 출석 모드 토글
    public void toggleAttendance(boolean active) {
        this.isAttendanceActive = active;
    }
}