package com.example.madcamp_2026_winter_MV.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class ScheduleRequestDto {
    private String title;
    private String content;
    private LocalDateTime startTime;
    private boolean isImportant;
}