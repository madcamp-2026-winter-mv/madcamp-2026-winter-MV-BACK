package com.example.madcamp_2026_winter_MV.config;

import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // 사용할 리포지토리들을 모두 선언해줘야 합니다.
    private final RoomRepository roomRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(String... args) {
        // 1. 기본 방 생성 (기획안의 MAD012)
        if (roomRepository.count() == 0) {
            roomRepository.save(Room.builder()
                    .name("2025 겨울학기")
                    .inviteCode("MAD012")
                    .build());
        }

        // 2. 카테고리 자동 생성
        if (categoryRepository.count() == 0) {
            String[] categories = {"자유", "질문", "팟모집", "정보공유", "채용공고"};
            for (String name : categories) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .build());
            }
        }

        // 3. 테스트용 게시글 생성 (멤버가 최소 한 명은 있어야 실행 가능)
        if (postRepository.count() == 0 && memberRepository.count() > 0) {
            Member admin = memberRepository.findAll().get(0); // 첫 번째 유저
            Room room = roomRepository.findAll().get(0);
            Category freeCat = categoryRepository.findAll().get(0); // 자유
            Category partyCat = categoryRepository.findAll().get(2); // 팟모집

            postRepository.save(Post.builder()
                    .title("2주차 프로젝트 후기 공유합니다")
                    .content("이번 주차에 React Native로 앱 만들면서 정말 많이 배웠어요...")
                    .member(admin)
                    .room(room)
                    .category(freeCat)
                    .type(PostType.NORMAL)
                    .build());

            postRepository.save(Post.builder()
                    .title("오늘 저녁 육비 시키실 분 구해요 (6시 30분 주문)")
                    .content("교반 같이 시키실분~ 같이먹어요!")
                    .member(admin)
                    .room(room)
                    .category(partyCat)
                    .type(PostType.PARTY)
                    .maxParticipants(4)
                    .currentParticipants(3)
                    .build());
        }
    }
}