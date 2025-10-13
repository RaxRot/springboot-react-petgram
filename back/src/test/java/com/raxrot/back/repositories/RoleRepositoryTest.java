package com.raxrot.back.repositories;

import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
//test from dari
@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByRoleName_returnsSavedRole() {
        // arrange
        Role userRole = new Role(AppRole.ROLE_USER);
        em.persist(userRole);
        em.flush();

        // act
        Optional<Role> found = roleRepository.findByRoleName(AppRole.ROLE_USER);

        // assert
        assertTrue(found.isPresent());
        assertNotNull(found.get().getRoleId());
        assertEquals(AppRole.ROLE_USER, found.get().getRoleName());
    }

    @Test
    void findByRoleName_returnsEmpty_whenNotExists() {
        // act
        Optional<Role> notFound = roleRepository.findByRoleName(AppRole.ROLE_ADMIN);

        // assert
        assertTrue(notFound.isEmpty());
    }
}