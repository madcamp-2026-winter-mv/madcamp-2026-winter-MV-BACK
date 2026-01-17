package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

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

    private LocalDateTime attendanceEndTime; // 출석 마감 시간

    @Builder.Default
    private int totalSessionCount = 0; // 분반 전체 세션 수 (출석률 계산용)

    public void addMember(Member member) {
        this.members.add(member);
        if (member.getRoom() != this) {
            member.setRoom(this);
        }
    }

    public void updateNotice(String notice) {
        this.notice = notice;
    }

    public void toggleAttendance(boolean active) {
        this.isAttendanceActive = active;
    }

    // --- 추가된 관리 메서드 ---

    // 분반 이름 수정
    public void updateName(String name) {
        this.name = name;
    }

    // 출석 시작 (마감 시간 설정)
    public void startAttendance(int minutes) {
        this.isAttendanceActive = true;
        this.attendanceEndTime = LocalDateTime.now().plusMinutes(minutes);
    }

    // 출석 종료
    public void stopAttendance() {
        this.isAttendanceActive = false;
        this.attendanceEndTime = null;
    }

    // 전체 세션 수 증가
    public void incrementTotalSessions() {
        this.totalSessionCount++;
    }

    // 발표자 업데이트
    public void updatePresenter(Long memberId) {
        this.currentPresenterId = memberId;
    }
}