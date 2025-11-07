package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.User;
import com.raxrot.back.services.ReportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("ReportServiceImpl PDF Generation Tests")
class ReportServiceImplTest {

    private final ReportService reportService = new ReportServiceImpl();

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should generate PDF containing correct user statistics and formatted data")
    void should_generate_pdf_with_correct_content() throws Exception {
        // given
        User user = new User();
        user.setUserName("vlad");

        UserStatsResponse stats = new UserStatsResponse(
                10, // posts
                55, // likes
                23, // comments
                1000, // views
                3, // pets
                40, // followers
                12, // following
                2599 // donations in cents = €25.99
        );

        // when
        ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(user, stats);

        // then
        assertThat(pdfStream).isNotNull();

        try (PDDocument document = PDDocument.load(pdfStream)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(0);

            String text = new PDFTextStripper().getText(document);

            // Basic structure
            assertThat(text).contains("Petgram User Analytics Report");
            assertThat(text).contains("User: vlad");

            // Verify each stat
            assertThat(text).contains("Total Posts");
            assertThat(text).contains("10");

            assertThat(text).contains("Total Likes");
            assertThat(text).contains("55");

            assertThat(text).contains("Total Comments");
            assertThat(text).contains("23");

            assertThat(text).contains("Total Views");
            assertThat(text).contains("1000");

            assertThat(text).contains("Total Pets");
            assertThat(text).contains("3");

            assertThat(text).contains("Total Followers");
            assertThat(text).contains("40");

            assertThat(text).contains("Total Following");
            assertThat(text).contains("12");

            // Donations Verification: comma OR dot
            assertThat(text).contains("Total Donations (€)");
            assertThat(text).containsAnyOf("25.99", "25,99");
        }
    }

    // ---------------------------------------------------------------
    @Test
    @DisplayName("Should generate a valid non-empty PDF file")
    void should_generate_non_empty_pdf() {
        // given
        User user = new User();
        user.setUserName("dasha");

        UserStatsResponse stats = new UserStatsResponse(0, 0, 0, 0, 0, 0, 0, 0);

        // when
        ByteArrayInputStream pdfStream = reportService.generateUserStatsPdf(user, stats);

        // then
        byte[] pdfBytes = pdfStream.readAllBytes();

        // **PDF always begins with "%PDF"**
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }
}
