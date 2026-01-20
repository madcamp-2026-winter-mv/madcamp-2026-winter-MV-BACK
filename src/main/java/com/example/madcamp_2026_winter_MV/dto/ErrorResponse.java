package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;
}