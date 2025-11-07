package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PollRequest;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.*;
import com.raxrot.back.repositories.*;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("PollServiceImpl Tests")
class PollServiceImplTest {

    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository optionRepository;
    @Mock private PollVoteRepository voteRepository;
    @Mock private PostRepository postRepository;
    @Mock private AuthUtil authUtil;

    @InjectMocks
    private PollServiceImpl pollService;

    private User me;
    private Post post;
    private Poll poll;
    private PollOption option;

    @BeforeEach
    void setUp() {
        me = new User();
        me.setUserId(1L);
        me.setUserName("vlad");

        post = new Post();
        post.setId(10L);
        post.setUser(me);

        poll = new Poll();
        poll.setId(100L);
        poll.setPost(post);
        poll.setQuestion("Favorite color?");
        poll.setOptions(List.of());

        option = new PollOption();
        option.setId(200L);
        option.setOptionText("Red");
        option.setVotes(0);
        option.setPoll(poll);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should create poll successfully")
    void should_create_poll_successfully() {
        PollRequest req = new PollRequest("Your favorite fruit?", List.of("Apple", "Banana"));

        given(authUtil.loggedInUser()).willReturn(me);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(pollRepository.save(any(Poll.class))).willAnswer(inv -> inv.getArgument(0));
        given(optionRepository.save(any(PollOption.class))).willAnswer(inv -> inv.getArgument(0));

        var response = pollService.createPoll(10L, req);

        assertThat(response.getQuestion()).isEqualTo("Your favorite fruit?");
        assertThat(response.getOptions()).hasSize(2);
        verify(pollRepository).save(any(Poll.class));
        verify(optionRepository, times(2)).save(any(PollOption.class));
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when post not found during poll creation")
    void should_throw_when_post_not_found_on_create() {
        PollRequest req = new PollRequest("Q", List.of("A", "B"));
        given(authUtil.loggedInUser()).willReturn(me);
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.createPoll(99L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Post not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when creating poll for someone else's post")
    void should_throw_when_creating_for_someone_elses_post() {
        User other = new User();
        other.setUserId(2L);
        post.setUser(other);

        PollRequest req = new PollRequest("Q", List.of("A", "B"));
        given(authUtil.loggedInUser()).willReturn(me);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> pollService.createPoll(10L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot add poll")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch poll successfully")
    void should_get_poll_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findByPost_Id(10L)).willReturn(Optional.of(poll));
        given(voteRepository.existsByPoll_IdAndUser_UserId(100L, 1L)).willReturn(false);

        var response = pollService.getPoll(10L);

        assertThat(response.getPollId()).isEqualTo(100L);
        assertThat(response.isVoted()).isFalse();
        verify(pollRepository).findByPost_Id(10L);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when poll not found")
    void should_throw_when_poll_not_found() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findByPost_Id(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.getPoll(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Poll not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should vote successfully in a poll")
    void should_vote_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findById(100L)).willReturn(Optional.of(poll));
        given(optionRepository.findById(200L)).willReturn(Optional.of(option));
        given(voteRepository.existsByPoll_IdAndUser_UserId(100L, 1L)).willReturn(false);

        pollService.vote(100L, 200L);

        verify(optionRepository).save(any(PollOption.class));
        verify(voteRepository).save(any(PollVote.class));
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when voting on non-existing poll")
    void should_throw_when_voting_poll_not_found() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.vote(999L, 200L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Poll not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when user already voted")
    void should_throw_when_already_voted() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findById(100L)).willReturn(Optional.of(poll));
        given(voteRepository.existsByPoll_IdAndUser_UserId(100L, 1L)).willReturn(true);

        assertThatThrownBy(() -> pollService.vote(100L, 200L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already voted")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(voteRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when option not found for voting")
    void should_throw_when_option_not_found() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findById(100L)).willReturn(Optional.of(poll));
        given(voteRepository.existsByPoll_IdAndUser_UserId(100L, 1L)).willReturn(false);
        given(optionRepository.findById(200L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.vote(100L, 200L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Option not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should delete poll successfully")
    void should_delete_poll_successfully() {
        poll.setOptions(List.of(option));
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findByPost_Id(10L)).willReturn(Optional.of(poll));
        given(voteRepository.findByPoll_Id(100L))
                .willAnswer(inv -> (Iterable<PollVote>) List.of(new PollVote()));


        pollService.deletePoll(10L);

        verify(voteRepository).deleteAll(anyList());
        verify(optionRepository).deleteAll(anyList());
        verify(pollRepository).delete(any(Poll.class));
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when deleting non-existing poll")
    void should_throw_when_deleting_poll_not_found() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findByPost_Id(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.deletePoll(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Poll not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when deleting someone else's poll")
    void should_throw_when_deleting_someone_elses_poll() {
        User other = new User();
        other.setUserId(2L);
        post.setUser(other);
        poll.setPost(post);

        given(authUtil.loggedInUser()).willReturn(me);
        given(pollRepository.findByPost_Id(10L)).willReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.deletePoll(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot delete someone else's poll")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }
}
