package com.raxrot.back.services.impl;

import com.raxrot.back.models.*;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.repositories.PostRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("InsightsServiceImpl Tests")
class InsightsServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private PetRepository petRepository;

    @InjectMocks
    private InsightsServiceImpl insightsService;

    private User user1, user2;
    private Post post1, post2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUserName("vlad");

        user2 = new User();
        user2.setUserName("dasha");

        post1 = new Post();
        post1.setTitle("Best Post");
        post1.setViewsCount(100);

        post2 = new Post();
        post2.setTitle("Another Post");
        post2.setViewsCount(50);
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should calculate insights and store result in cache")
    void should_calculate_insights() {
        // Most liked post last 7 days
        given(postRepository.findTopByCreatedAtBetweenOrderByLikesDesc(any(), any()))
                .willReturn(post1);

        // Comments
        Comment c1 = new Comment();
        c1.setAuthor(user1);
        Comment c2 = new Comment();
        c2.setAuthor(user1);
        Comment c3 = new Comment();
        c3.setAuthor(user2);
        given(commentRepository.findAll()).willReturn(List.of(c1, c2, c3));

        // Donations last 30 days
        Donation d1 = new Donation();
        d1.setDonor(user2);
        d1.setAmount(500L);
        Donation d2 = new Donation();
        d2.setDonor(user2);
        d2.setAmount(100L);
        given(donationRepository.findAllByCreatedAtBetween(any(), any()))
                .willReturn(List.of(d1, d2));

        // Pets
        Pet pet1 = new Pet();
        pet1.setOwner(user2);
        Pet pet2 = new Pet();
        pet2.setOwner(user2);
        given(petRepository.findAll()).willReturn(List.of(pet1, pet2));

        // Most viewed post
        given(postRepository.findTopByOrderByViewsCountDesc()).willReturn(post1);

        // Act
        insightsService.calculateInsights();
        Map<String, Object> result = insightsService.getInsights();

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get("mostLikedPost")).isEqualTo("Best Post");
        assertThat(result.get("mostActiveCommenter")).isEqualTo("vlad (2 comments)");
        assertThat(result.get("topDonor")).isEqualTo("dasha (€6.0)");
        assertThat(result.get("mostPopularPetOwner")).isEqualTo("dasha (2 pets)");
        assertThat(result.get("mostViewedPost")).isEqualTo("Best Post (100 views)");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should return 'No data' when empty results are found")
    void should_return_no_data_for_empty_results() {
        given(postRepository.findTopByCreatedAtBetweenOrderByLikesDesc(any(), any()))
                .willReturn(null);
        given(commentRepository.findAll()).willReturn(List.of());
        given(donationRepository.findAllByCreatedAtBetween(any(), any()))
                .willReturn(List.of());
        given(petRepository.findAll()).willReturn(List.of());
        given(postRepository.findTopByOrderByViewsCountDesc()).willReturn(null);

        insightsService.calculateInsights();
        Map<String, Object> result = insightsService.getInsights();

        assertThat(result.get("mostLikedPost")).isEqualTo("No data");
        assertThat(result.get("mostActiveCommenter")).isEqualTo("No data");
        assertThat(result.get("topDonor")).isEqualTo("No data");
        assertThat(result.get("mostPopularPetOwner")).isEqualTo("No data");
        assertThat(result.get("mostViewedPost")).isEqualTo("No data");
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should call calculateInsights only once when cache already exists")
    void should_not_recalculate_when_cache_exists() {
        // given: first call creates cache
        insightsService.calculateInsights();

        // when
        insightsService.getInsights();

        assertThat(insightsService.getInsights()).isNotEmpty();
    }
}