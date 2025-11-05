package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("AdminServiceImpl Tests")
class AdminServiceImplTest {

    @Mock private DonationRepository donationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User donor;
    private User receiver;
    private Donation donation;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setUserId(1L);
        donor.setUserName("vlad");

        receiver = new User();
        receiver.setUserId(2L);
        receiver.setUserName("dasha");

        donation = new Donation(
                10L,
                donor,
                receiver,
                100L,
                "EUR",
                LocalDateTime.of(2025, 10, 24, 12, 0)
        );
    }

    @Test
    @DisplayName("Should return mapped list of DonationResponse sorted by date")
    void should_return_all_donations_sorted_and_mapped() {
        // given
        given(donationRepository.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(donation));

        // when
        List<DonationResponse> responses = adminService.getAllDonations();

        // then
        assertThat(responses)
                .isNotNull()
                .hasSize(1);

        DonationResponse dto = responses.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getDonorUsername()).isEqualTo("vlad");
        assertThat(dto.getReceiverUsername()).isEqualTo("dasha");
        assertThat(dto.getAmount()).isEqualTo(100L);
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 10, 24, 12, 0));

        verify(donationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should count total users correctly")
    void should_count_total_users() {
        // given
        given(userRepository.count()).willReturn(15L);

        // when
        long count = adminService.countUsers();

        // then
        assertThat(count).isEqualTo(15L);
        verify(userRepository).count();
    }

    @Test
    @DisplayName("Should count total donations correctly")
    void should_count_total_donations() {
        // given
        given(donationRepository.count()).willReturn(5L);

        // when
        long count = adminService.countDonations();

        // then
        assertThat(count).isEqualTo(5L);
        verify(donationRepository).count();
    }

    @Test
    @DisplayName("Should count total comments correctly")
    void should_count_total_comments() {
        // given
        given(commentRepository.count()).willReturn(30L);

        // when
        long count = adminService.countComments();

        // then
        assertThat(count).isEqualTo(30L);
        verify(commentRepository).count();
    }

    @Test
    @DisplayName("Should count total posts correctly")
    void should_count_total_posts() {
        // given
        given(postRepository.count()).willReturn(8L);

        // when
        long count = adminService.countPosts();

        // then
        assertThat(count).isEqualTo(8L);
        verify(postRepository).count();
    }
}