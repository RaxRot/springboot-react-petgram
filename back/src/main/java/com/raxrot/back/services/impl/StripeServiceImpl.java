package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CheckoutRequest;
import com.raxrot.back.dtos.StripeResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.EmailService;
import com.raxrot.back.services.StripeService;
import com.raxrot.back.utils.AuthUtil;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public StripeResponse createCheckoutSession(CheckoutRequest req) {
       //who pay
        User donor = authUtil.loggedInUser();

        // who recive
        User author = userRepository.findById(req.getAuthorId())
                .orElseThrow(() -> new ApiException("Author not found", HttpStatus.NOT_FOUND));

        if (author.getUserId().equals(donor.getUserId())) {
            throw new ApiException("You cannot donate to yourself", HttpStatus.BAD_REQUEST);
        }
        if (req.getAmount() < 100) {
            throw new ApiException("Minimum amount is 1.00", HttpStatus.BAD_REQUEST);
        }

        // get sesseion
        try {
            String currency = req.getCurrency().toLowerCase();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(req.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(req.getCancelUrl())
                    //sent metadata
                    .putMetadata("authorId", String.valueOf(author.getUserId()))
                    .putMetadata("authorUsername", author.getUserName())
                    .putMetadata("donorId", String.valueOf(donor.getUserId()))
                    .putMetadata("donorUsername", donor.getUserName())
                    // donat
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(req.getAmount()) // cents
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

             sendEmailToDonationReceiver(req, author, donor);
             sendEmailToDonationSender(donor, author);


            return new StripeResponse(
                    "success",
                    "Checkout session created",
                    session.getId(),
                    session.getUrl()
            );
        } catch (Exception e) {
            return new StripeResponse("error", e.getMessage(), null, null);
        }
    }

    private void sendEmailToDonationSender(User donor, User author) {
        emailService.sendEmail(
                donor.getEmail(),
                "🙏 Thank you for your donation! 🙏",
                "✨ Dear " + donor.getUserName() + ",\n\n" +
                        "💝 Your donation has been successfully sent to *" + author.getUserName() + "* 🎉\n\n" +
                        "We truly appreciate your support! 🌟"
        );
    }

    private void sendEmailToDonationReceiver(CheckoutRequest req, User author, User donor) {
        emailService.sendEmail(
                author.getEmail(),
                "🎉 You received a donation! 🎉",
                "🙌 Hi " + author.getUserName() + "!\n\n" +
                        "💖 User *" + donor.getUserName() + "* just donated you " + (req.getAmount() / 100) + "€ 🎁\n\n" +
                        "Thank you for inspiring others! 🚀"
        );
    }
}
