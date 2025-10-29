package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.UserStatsResponse;
import com.raxrot.back.models.User;
import com.raxrot.back.services.ReportService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Override
    public ByteArrayInputStream generateUserStatsPdf(User user, UserStatsResponse stats) {
        log.info("Generating PDF report for user: {}", user.getUserName());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Petgram User Analytics Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            // User info
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("User: " + user.getUserName(), infoFont));
            document.add(new Paragraph("Generated: " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), infoFont));

            document.add(Chunk.NEWLINE);

            // Stats table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addRow(table, "Total Posts", stats.getTotalPosts());
            addRow(table, "Total Likes", stats.getTotalLikes());
            addRow(table, "Total Comments", stats.getTotalComments());
            addRow(table, "Total Views", stats.getTotalViews());
            addRow(table, "Total Pets", stats.getTotalPets());
            addRow(table, "Total Followers", stats.getTotalFollowers());
            addRow(table, "Total Following", stats.getTotalFollowing());
            double donationsInEuro = stats.getTotalDonationsReceived() / 100.0;
            addRow(table, "Total Donations (€)", String.format("%.2f", donationsInEuro));


            document.add(table);
            document.close();

            log.info("PDF report generated successfully for user: {}", user.getUserName());

        } catch (Exception e) {
            log.error("Error while generating PDF report: {}", e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addRow(PdfPTable table, String key, Object value) {
        PdfPCell cell1 = new PdfPCell(new Phrase(key));
        PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(value)));
        cell1.setPadding(5);
        cell2.setPadding(5);
        table.addCell(cell1);
        table.addCell(cell2);
    }

}
