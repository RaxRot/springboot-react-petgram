package com.raxrot.back.repositories;

import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("should return saved role when role exists in database")
    void findByRoleName_returnsSavedRole() {
        // arrange
        Role userRole = new Role(AppRole.ROLE_USER);
        roleRepository.save(userRole);

        // act
        Optional<Role> found = roleRepository.findByRoleName(AppRole.ROLE_USER);

        // assert
        assertTrue(found.isPresent(), "Role should be found");
        assertNotNull(found.get().getRoleId(), "Role ID should not be null");
        assertEquals(AppRole.ROLE_USER, found.get().getRoleName(), "Role name should match");
    }

    @Test
    @DisplayName("should return empty when role does not exist in database")
    void findByRoleName_returnsEmpty_whenNotExists() {
        // act
        Optional<Role> notFound = roleRepository.findByRoleName(AppRole.ROLE_ADMIN);

        // assert
        assertTrue(notFound.isEmpty(), "Result should be empty when role not found");
    }
}