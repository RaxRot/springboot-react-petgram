package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PostPageResponse;
import com.raxrot.back.dtos.PostResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Bookmark;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.BookmarkRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.BookMarkService;
import com.raxrot.back.utils.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookMarkServiceImpl implements BookMarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public void addBookmark(Long postId) {
        User me = authUtil.loggedInUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));

        if (bookmarkRepository.existsByPost_IdAndUser_UserId(postId, me.getUserId())) {
            return;
        }

        Bookmark b = new Bookmark();
        b.setUser(me);
        b.setPost(post);
        bookmarkRepository.save(b);
    }

    @Transactional
    @Override
    public void removeBookmark(Long postId) {
        User me = authUtil.loggedInUser();
        bookmarkRepository.deleteByPost_IdAndUser_UserId(postId, me.getUserId());
    }

    @Override
    public PostPageResponse getMyBookmarks(int page, int size, String sortBy, String sortOrder) {
        User me = authUtil.loggedInUser();

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Bookmark> bmPage = bookmarkRepository.findAllByUser_UserId(me.getUserId(), pageable);

        List<PostResponse> content = bmPage.getContent().stream()
                .map(bm -> modelMapper.map(bm.getPost(), PostResponse.class))
                .toList();

        PostPageResponse resp = new PostPageResponse();
        resp.setContent(content);
        resp.setPageNumber(bmPage.getNumber());
        resp.setPageSize(bmPage.getSize());
        resp.setTotalElements(bmPage.getTotalElements());
        resp.setTotalPages(bmPage.getTotalPages());
        resp.setLastPage(bmPage.isLast());

        return resp;
    }
}
