package com.seproject.member.application;

import com.seproject.board.comment.domain.repository.CommentLikeRepository;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.post.domain.repository.PostLikeRepository;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.FrameType;
import com.seproject.member.domain.model.Tier;
import com.seproject.member.domain.repository.FrameRepository;
import com.seproject.member.domain.repository.MemberFrameRepository;
import com.seproject.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TierBatchService {

    private final MemberRepository memberRepository;
    private final FrameRepository frameRepository;
    private final MemberFrameRepository memberFrameRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;

    // 티어별 프레임 이름 매핑
    private static final Map<Tier, String> TIER_FRAME_NAMES = Map.of(
            Tier.BRONZE,   "브론즈 프레임",
            Tier.SILVER,   "실버 프레임",
            Tier.GOLD,     "골드 프레임",
            Tier.PLATINUM, "플래티넘 프레임",
            Tier.DIAMOND,  "다이아몬드 프레임"
    );

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateTiersAndGrantFrames() {
        log.info("[TierBatch] 티어 갱신 시작");
        List<Member> members = memberRepository.findAll();

        for (Member member : members) {
            Long memberId = member.getBoardUserId();

            long postCount    = postRepository.countByAuthorId(memberId);
            long commentCount = commentRepository.countByAuthorId(memberId);
            long postLikes    = postLikeRepository.countLikesReceivedByMember(memberId);
            long commentLikes = commentLikeRepository.countLikesReceivedByMember(memberId);

            long score = postCount * 5 + commentCount * 2 + postLikes * 3 + commentLikes;
            Tier newTier = Tier.of(score);

            member.updateTierAndScore(score);

            // 달성한 티어 이하의 모든 티어 프레임 지급 (누적)
            for (Tier tier : Tier.values()) {
                if (tier.ordinal() > newTier.ordinal()) break;
                grantTierFrameIfAbsent(member, tier);
            }
        }

        log.info("[TierBatch] 티어 갱신 완료 - {}명 처리", members.size());
    }

    private void grantTierFrameIfAbsent(Member member, Tier tier) {
        String frameName = TIER_FRAME_NAMES.get(tier);
        Optional<Frame> frameOpt = frameRepository.findByName(frameName);

        if (frameOpt.isEmpty()) {
            log.warn("[TierBatch] 프레임 없음: {}", frameName);
            return;
        }

        Frame frame = frameOpt.get();
        if (!memberFrameRepository.existsByMemberAndFrame(member, frame)) {
            memberFrameRepository.save(
                    com.seproject.member.domain.model.MemberFrame.builder()
                            .member(member)
                            .frame(frame)
                            .build()
            );
        }
    }
}
