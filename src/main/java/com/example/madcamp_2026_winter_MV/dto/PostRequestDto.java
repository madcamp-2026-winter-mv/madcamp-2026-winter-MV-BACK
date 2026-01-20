package com.example.madcamp_2026_winter_MV.dto;

import com.example.madcamp_2026_winter_MV.entity.PostType;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class PostRequestDto {
    private Long roomId;
    private Long categoryId;
    private String title;
    private String content;
    private PostType type;
    private Integer maxParticipants;
    private List<String> voteContents;
    private Boolean isAnonymous;
}