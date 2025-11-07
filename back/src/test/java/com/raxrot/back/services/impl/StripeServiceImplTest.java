package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.EmailService;
import com.raxrot.back.utils.AuthUtil;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("StripeServiceImpl Tests")
class StripeServiceImplTest {

    @Mock private AuthUtil authUtil;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private DonationRepository donationRepository;

    @InjectMocks
    private StripeServiceImpl stripeService;

    private User donor;
    private User author;
    private CheckoutRequest request;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setUserId(1L);
        donor.setUserName("vlad");
        donor.setEmail("vlad@test.com");

        author = new User();
        author.setUserId(2L);
        author.setUserName("dasha");
        author.setEmail("dasha@test.com");

        request = new CheckoutRequest();
        request.setAuthorId(2L);
        request.setAmount(500L);
        request.setCurrency("EUR");
        request.setSuccessUrl("https://ok/success");
        request.setCancelUrl("https://ok/cancel");
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should create Stripe checkout session successfully")
    void should_create_checkout_session_successfully() throws Exception {
        given(authUtil.loggedInUser()).willReturn(donor);
        given(userRepository.findById(2L)).willReturn(Optional.of(author));
        given(donationRepository.save(any(Donation.class))).willAnswer(inv -> inv.getArgument(0));

        Session mockSession = mock(Session.class);
        given(mockSession.getId()).willReturn("sess_123");
        given(mockSession.getUrl()).willReturn("https://stripe.com/session");

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);


            StripeResponse resp = stripeService.createCheckoutSession(request);

            assertThat(resp.getStatus()).isEqualTo("success");
            assertThat(resp.getSessionId()).isEqualTo("sess_123");
            verify(donationRepository).save(any(Donation.class));
            verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
        }
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when author not found")
    void should_throw_when_author_not_found() {
        given(authUtil.loggedInUser()).willReturn(donor);
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stripeService.createCheckoutSession(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Author not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when donor tries to donate to themselves")
    void should_throw_when_donating_to_self() {
        given(authUtil.loggedInUser()).willReturn(donor);
        request.setAuthorId(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(donor));

        assertThatThrownBy(() -> stripeService.createCheckoutSession(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot donate to yourself")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should throw when amount less than 100 cents")
    void should_throw_when_amount_too_low() {
        given(authUtil.loggedInUser()).willReturn(donor);
        given(userRepository.findById(2L)).willReturn(Optional.of(author));
        request.setAmount(50L);

        assertThatThrownBy(() -> stripeService.createCheckoutSession(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Minimum amount is 1.00")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Should return error StripeResponse when Stripe API fails")
    void should_return_error_response_on_stripe_failure() throws Exception {
        given(authUtil.loggedInUser()).willReturn(donor);
        given(userRepository.findById(2L)).willReturn(Optional.of(author));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new RuntimeException("Stripe failure"));


            StripeResponse resp = stripeService.createCheckoutSession(request);

            assertThat(resp.getStatus()).isEqualTo("error");
            assertThat(resp.getMessage()).contains("Stripe failure");
            verify(donationRepository, never()).save(any());
        }
    }
}
