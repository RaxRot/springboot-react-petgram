package com.raxrot.back.utils;

import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private final UserRepository userRepository;
    public AuthUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User Not Found with username: " + authentication.getName()
                ));
    }

    public String loggedInEmail() {
        return getCurrentUserEntity().getEmail();
    }

    public Long loggedInUserId() {
        return getCurrentUserEntity().getUserId();
    }

    public User loggedInUser() {
        return getCurrentUserEntity();
    }
}