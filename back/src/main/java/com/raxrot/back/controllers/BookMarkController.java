package com.raxrot.back.controllers;

import com.raxrot.back.configs.AppConstants;
import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.services.BookMarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookMarkController {
    private final BookMarkService bookmarkService;

    @PostMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<String> addBookmark(@PathVariable Long postId) {
        bookmarkService.addBookmark(postId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Bookmark added");
    }

    @DeleteMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long postId) {
        bookmarkService.removeBookmark(postId);
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/user/bookmarks")
    public ResponseEntity<PostPageResponse> getBookmarks(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CREATED_AT) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        PostPageResponse page = bookmarkService.getMyBookmarks(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(page);
    }
}