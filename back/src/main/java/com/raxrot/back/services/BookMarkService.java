package com.raxrot.back.services;

import com.raxrot.back.dtos.PostPageResponse;

public interface BookMarkService {
    void addBookmark(Long postId);
    void removeBookmark(Long postId);
    PostPageResponse getMyBookmarks(int page, int size, String sortBy, String sortOrder);
}
