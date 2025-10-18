package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.CommentPageResponse;
import com.raxrot.back.dtos.CommentRequest;
import com.raxrot.back.dtos.CommentResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Comment;
import com.raxrot.back.models.Post;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.CommentRepository;
import com.raxrot.back.repositories.PostRepository;
import com.raxrot.back.services.CommentService;
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
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;


    @Transactional
    @Override
    public CommentResponse addComment(Long postId, CommentRequest req) {
        User user=authUtil.loggedInUser();
        if (user.isBanned()){
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }
        Post post=postRepository.findById(postId)
                .orElseThrow(()->new ApiException("Post not found", HttpStatus.NOT_FOUND));

        Comment comment=modelMapper.map(req, Comment.class);
        comment.setPost(post);
        comment.setAuthor(user);
        Comment savedComment=commentRepository.save(comment);



        return modelMapper.map(savedComment, CommentResponse.class);
    }

    @Override
    public CommentPageResponse getComments(Long postId,
                                           Integer page,
                                           Integer size,
                                           String sortBy,
                                           String sortOrder) {
        if (!postRepository.existsById(postId)) {
            throw new ApiException("Post not found", HttpStatus.NOT_FOUND);
        }

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> commentPage = commentRepository.findAllByPost_Id(postId, pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(c -> modelMapper.map(c, CommentResponse.class))
                .toList();

        CommentPageResponse resp = new CommentPageResponse();
        resp.setContent(comments);
        resp.setPageNumber(commentPage.getNumber());
        resp.setPageSize(commentPage.getSize());
        resp.setTotalElements(commentPage.getTotalElements());
        resp.setTotalPages(commentPage.getTotalPages());
        resp.setLastPage(commentPage.isLast());
        return resp;
    }

    @Transactional
    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest req) {
        User user=authUtil.loggedInUser();
        if (user.isBanned()){
            throw new ApiException("User is banned", HttpStatus.FORBIDDEN);
        }
        Comment comment=commentRepository.findById(commentId)
                .orElseThrow(()->new ApiException("Comment not found", HttpStatus.NOT_FOUND));
        if (!comment.getAuthor().getUserId().equals(user.getUserId())){
            throw new ApiException("You are not allowed to update this comment", HttpStatus.FORBIDDEN);
        }
        comment.setText(req.getText());
        Comment updatedComment=commentRepository.save(comment);
        return modelMapper.map(updatedComment, CommentResponse.class);
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId) {
        User user=authUtil.loggedInUser();
        Comment comment=commentRepository.findById(commentId)
                .orElseThrow(()->new ApiException("Comment not found", HttpStatus.NOT_FOUND));
        boolean isAdmin= user.getRoles().stream().anyMatch(r -> r.getRoleName() == AppRole.ROLE_ADMIN);
        boolean isOwner=comment.getAuthor().getUserId().equals(user.getUserId());
        if (isAdmin || isOwner){
            commentRepository.delete(comment);
        }else{
            throw new ApiException("You are not allowed to delete this comment", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public CommentPageResponse getAllComments(Integer page, Integer size, String sortBy, String sortOrder) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> commentPage = commentRepository.findAll(pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(c -> modelMapper.map(c, CommentResponse.class))
                .toList();

        CommentPageResponse resp = new CommentPageResponse();
        resp.setContent(comments);
        resp.setPageNumber(commentPage.getNumber());
        resp.setPageSize(commentPage.getSize());
        resp.setTotalElements(commentPage.getTotalElements());
        resp.setTotalPages(commentPage.getTotalPages());
        resp.setLastPage(commentPage.isLast());
        return resp;
    }

}
