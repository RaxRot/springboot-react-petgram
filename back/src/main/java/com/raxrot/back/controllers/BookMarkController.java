package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.services.BookMarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Bookmarks",
        description = "Endpoints for managing user bookmarks on posts."
)
@SecurityRequirement(name = "Bearer Authentication")
public class BookMarkController {

    private final BookMarkService bookmarkService;

    @Operation(
            summary = "Add bookmark",
            description = "Adds the specified post to the current user's bookmarks.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Bookmark successfully added"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @PostMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<String> addBookmark(
            @Parameter(description = "ID of the post to bookmark") @PathVariable Long postId) {
        log.info("⭐ Add bookmark request received for post ID: {}", postId);
        bookmarkService.addBookmark(postId);
        log.info("✅ Bookmark successfully added for post ID: {}", postId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Bookmark added");
    }

    @Operation(
            summary = "Remove bookmark",
            description = "Removes the specified post from the user's bookmarks.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Bookmark successfully removed"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    @DeleteMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<Void> deleteBookmark(
            @Parameter(description = "ID of the post to remove from bookmarks") @PathVariable Long postId) {
        log.info("🗑️ Remove bookmark request received for post ID: {}", postId);
        bookmarkService.removeBookmark(postId);
        log.info("✅ Bookmark successfully removed for post ID: {}", postId);
        return ResponseEntity.noContent().build(); // 204
    }

    @Operation(
            summary = "Get user bookmarks",
            description = "Returns a paginated list of posts that the current user has bookmarked.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Bookmarks successfully retrieved",
                            content = @Content(schema = @Schema(implementation = PostPageResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/user/bookmarks")
    public ResponseEntity<PostPageResponse> getBookmarks(
            @Parameter(description = "Page number (default = 0)")
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @Parameter(description = "Page size (default = 10)")
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @Parameter(description = "Sort field (default = createdAt)")
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @Parameter(description = "Sort order (asc or desc, default = asc)")
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        log.info("📖 Fetching bookmarks | pageNumber={}, pageSize={}, sortBy={}, sortOrder={}",
                pageNumber, pageSize, sortBy, sortOrder);

        PostPageResponse page = bookmarkService.getMyBookmarks(pageNumber, pageSize, sortBy, sortOrder);

        log.info("✅ Retrieved {} bookmarks on page {}", page.getContent().size(), pageNumber);
        return ResponseEntity.ok(page);
    }
}
