package com.raxrot.back.configs;

import com.raxrot.back.repositories.StoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class StoryCleanupJob {

    private final StoryRepository storyRepository;

    //@Scheduled(cron = "0 * * * * *") 1 min
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanup() {
        storyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
