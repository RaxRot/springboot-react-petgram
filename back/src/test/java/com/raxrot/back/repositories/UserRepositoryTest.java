package com.raxrot.back.repositories;

import com.raxrot.back.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should return saved user when searching by username")
    void findByUserName_returnsSavedUser() {
        // arrange
        User user = new User("darya", "darya@example.com", "12345");
        userRepository.save(user);

        // act
        Optional<User> found = userRepository.findByUserName("darya");

        // assert
        assertTrue(found.isPresent(), "User should be found by username");
        assertEquals("darya", found.get().getUserName(), "Username should match");
        assertEquals("darya@example.com", found.get().getEmail(), "Email should match");
    }

    @ParameterizedTest(name = "findByUserName('{0}') should return user with email {1}")
    @CsvSource({
            "john, john@example.com",
            "mike, mike@example.com",
            "sarah, sarah@example.com"
    })
    @DisplayName("should correctly find multiple users by username (parameterized)")
    void findByUserName_multipleUsers(String username, String email) {
        // arrange
        User user = new User(username, email, "pwd");
        userRepository.save(user);

        // act
        Optional<User> found = userRepository.findByUserName(username);

        // assert
        assertTrue(found.isPresent(), "User should be found by username");
        assertEquals(email, found.get().getEmail(), "Email should match");
    }

    @Test
    @DisplayName("should return saved user when searching by email")
    void findByEmail_returnsSavedUser() {
        // arrange
        User user = new User("john", "john@example.com", "pass");
        userRepository.save(user);

        // act
        Optional<User> found = userRepository.findByEmail("john@example.com");

        // assert
        assertTrue(found.isPresent(), "User should be found by email");
        assertEquals("john", found.get().getUserName(), "Username should match");
    }

    @Test
    @DisplayName("should return true when user exists by email")
    void existsByEmail_returnsTrue_whenUserExists() {
        // arrange
        User user = new User("mike", "mike@example.com", "pwd");
        userRepository.save(user);

        // act
        boolean exists = userRepository.existsByEmail("mike@example.com");

        // assert
        assertTrue(exists, "existsByEmail should return true for existing user");
    }

    @Test
    @DisplayName("should return false when user does not exist by email")
    void existsByEmail_returnsFalse_whenUserNotExists() {
        boolean exists = userRepository.existsByEmail("nope@example.com");
        assertFalse(exists, "existsByEmail should return false when user not found");
    }

    @Test
    @DisplayName("should return true when user exists by username")
    void existsByUserName_returnsTrue_whenUserExists() {
        // arrange
        User user = new User("alex", "alex@example.com", "pwd");
        userRepository.save(user);

        // act
        boolean exists = userRepository.existsByUserName("alex");

        // assert
        assertTrue(exists, "existsByUserName should return true for existing user");
    }

    @Test
    @DisplayName("should return false when user does not exist by username")
    void existsByUserName_returnsFalse_whenUserNotExists() {
        boolean exists = userRepository.existsByUserName("ghost");
        assertFalse(exists, "existsByUserName should return false when user not found");
    }
}