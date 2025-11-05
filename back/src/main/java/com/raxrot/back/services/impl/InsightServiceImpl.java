package com.raxrot.back.services.impl;

import com.raxrot.back.models.Donation;
import com.raxrot.back.models.Post;
import com.raxrot.back.repositories.*;
import com.raxrot.back.services.InsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightServiceImpl implements InsightsService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final DonationRepository donationRepository;
    private final PetRepository petRepository;

    private Map<String, Object> cache = new HashMap<>();

    /**
     * Обновление инсайтов каждые сутки в полночь.
     * Метрики считаются за последние 7 дней.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void calculateInsights() {
        log.info("🔄 Recalculating 7-day admin insights...");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();

        // 🏆 Most liked post (last 7 days)
        Post mostLiked = postRepository.findTopByCreatedAtBetweenOrderByLikesDesc(sevenDaysAgo, now);
        String mostLikedPost = (mostLiked != null)
                ? mostLiked.getTitle()
                : "No data";

        // 💬 Most active commenter (all time)
        var commenterOpt = commentRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getAuthor().getUserName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());
        String mostActiveCommenter = commenterOpt
                .map(e -> e.getKey() + " (" + e.getValue() + " comments)")
                .orElse("No data");

        // 💸 Top donor (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        var donations = donationRepository.findAllByCreatedAtBetween(thirtyDaysAgo, now);
        var donorOpt = donations.stream()
                .collect(Collectors.groupingBy(d -> d.getDonor().getUserName(),
                        Collectors.summingLong(Donation::getAmount)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());
        String topDonor = donorOpt
                .map(e -> e.getKey() + " (€" + e.getValue() / 100.0 + ")")
                .orElse("No data");

        // 🐶 Most popular pet owner (all time)
        var popularOwnerOpt = petRepository.findAll().stream()
                .collect(Collectors.groupingBy(p -> p.getOwner().getUserName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());
        String mostPopularPetOwner = popularOwnerOpt
                .map(e -> e.getKey() + " (" + e.getValue() + " pets)")
                .orElse("No data");

        // 🔥 Most viewed post (all time)
        Post mostViewed = postRepository.findTopByOrderByViewsCountDesc();
        String mostViewedPost = (mostViewed != null)
                ? mostViewed.getTitle() + " (" + mostViewed.getViewsCount() + " views)"
                : "No data";

        // 🧮 Build cache map
        cache = Map.of(
                "mostLikedPost", mostLikedPost,
                "mostActiveCommenter", mostActiveCommenter,
                "topDonor", topDonor,
                "mostPopularPetOwner", mostPopularPetOwner,
                "mostViewedPost", mostViewedPost
        );

        log.info("✅ Admin insights successfully recalculated: {}", cache);
    }

    @Override
    public Map<String, Object> getInsights() {
        if (cache.isEmpty()) calculateInsights();
        return cache;
    }
}
