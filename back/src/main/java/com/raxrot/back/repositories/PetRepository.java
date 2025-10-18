package com.raxrot.back.repositories;

import com.raxrot.back.models.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Page<Pet> findAllByOwner_UserId(Long userId, Pageable pageable);
    Page<Pet> findAllByOwner_UserName(String username, Pageable pageable);

    long countByOwner_UserId(Long userId);
}

