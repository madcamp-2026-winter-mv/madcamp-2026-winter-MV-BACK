package com.example.madcamp_2026_winter_MV.scheduler;

import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.entity.PostType;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import com.example.madcamp_2026_winter_MV.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component // 스프링이 자동으로 관리하도록 등록
@RequiredArgsConstructor
public class VoteScheduler {

    private final PostRepository postRepository;
    private final NotificationService notificationService;

    // 10분마다(600,000ms) 실행하며 24시간 지난 투표를 찾음
    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void autoCloseVotes() {
        LocalDateTime limitTime = LocalDateTime.now().minusHours(24);

        // 아직 안 닫혔고(isClosed=false), 24시간 이전에 생성된 투표글 조회
        List<Post> expiredVotes = postRepository.findByTypeAndIsClosedFalseAndCreatedAtBefore(
                PostType.VOTE, limitTime);

        for (Post post : expiredVotes) {
            post.setClosed(true); // 마감 상태로 변경

            // 작성자에게 SSE 알림 발송
            notificationService.createNotification(
                    post.getMember(),
                    post,
                    "투표 [" + post.getTitle() + "]가 종료되었습니다."
            );
        }
    }
}