package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Donation;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.DonationRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.EmailService;
import com.raxrot.back.services.StripeService;
import com.raxrot.back.utils.AuthUtil;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DonationRepository donationRepository;

    @Override
    public StripeResponse createCheckoutSession(CheckoutRequest req) {
        log.info("Starting Stripe checkout session creation for authorId={} and amount={}{}", req.getAuthorId(), req.getAmount(), req.getCurrency());


        User donor = authUtil.loggedInUser();
        log.debug("Logged in user '{}' (ID={}) is making a donation", donor.getUserName(), donor.getUserId());


        User author = userRepository.findById(req.getAuthorId())
                .orElseThrow(() -> {
                    log.error("Author with ID {} not found", req.getAuthorId());
                    return new ApiException("Author not found", HttpStatus.NOT_FOUND);
                });

        if (author.getUserId().equals(donor.getUserId())) {
            log.warn("User '{}' attempted to donate to themselves", donor.getUserName());
            throw new ApiException("You cannot donate to yourself", HttpStatus.BAD_REQUEST);
        }

        if (req.getAmount() < 100) {
            log.warn("User '{}' tried to donate less than the minimum amount ({} cents)", donor.getUserName(), req.getAmount());
            throw new ApiException("Minimum amount is 1.00", HttpStatus.BAD_REQUEST);
        }

        try {
            String currency = req.getCurrency().toLowerCase();

            log.info("Creating Stripe session for donor='{}' -> author='{}' ({}{})",
                    donor.getUserName(), author.getUserName(), req.getAmount() / 100.0, currency.toUpperCase());

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(req.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(req.getCancelUrl())

                    .putMetadata("authorId", String.valueOf(author.getUserId()))
                    .putMetadata("authorUsername", author.getUserName())
                    .putMetadata("donorId", String.valueOf(donor.getUserId()))
                    .putMetadata("donorUsername", donor.getUserName())

                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(req.getAmount()) // в центах
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Donation to @" + author.getUserName())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            log.info("Stripe checkout session created successfully (sessionId={})", session.getId());

            Donation donation = new Donation();
            donation.setDonor(donor);
            donation.setReceiver(author);
            donation.setAmount(req.getAmount());
            donation.setCurrency(req.getCurrency());
            donationRepository.save(donation);
            log.info("Donation record saved: donor='{}' -> receiver='{}', amount={}{}", donor.getUserName(), author.getUserName(), req.getAmount(), req.getCurrency());

            sendEmailToDonationReceiver(req, author, donor);
            sendEmailToDonationSender(donor, author);
            log.info("Notification emails sent to donor and receiver");

            return new StripeResponse(
                    "success",
                    "Checkout session created",
                    session.getId(),
                    session.getUrl()
            );
        } catch (Exception e) {
            log.error("Error creating Stripe checkout session: {}", e.getMessage(), e);
            return new StripeResponse("error", e.getMessage(), null, null);
        }
    }

    private void sendEmailToDonationSender(User donor, User author) {
        log.debug("Sending confirmation email to donor '{}'", donor.getUserName());
        emailService.sendEmail(
                donor.getEmail(),
                "🙏 Thank you for your donation! 🙏",
                "✨ Dear " + donor.getUserName() + ",\n\n" +
                        "💝 Your donation has been successfully sent to *" + author.getUserName() + "* 🎉\n\n" +
                        "We truly appreciate your support! 🌟"
        );
    }

    private void sendEmailToDonationReceiver(CheckoutRequest req, User author, User donor) {
        log.debug("Sending notification email to donation receiver '{}'", author.getUserName());
        emailService.sendEmail(
                author.getEmail(),
                "🎉 You received a donation! 🎉",
                "🙌 Hi " + author.getUserName() + "!\n\n" +
                        "💖 User *" + donor.getUserName() + "* just donated you " + (req.getAmount() / 100) + "€ 🎁\n\n" +
                        "Thank you for inspiring others! 🚀"
        );
    }
}
