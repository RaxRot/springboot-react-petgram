package com.raxrot.back.services;

import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PetService {
    PetResponse createPet(PetRequest req, MultipartFile file);
    PetPageResponse getMyPets(int page, int size);
    PetPageResponse getPetsByUsername(String username, int page, int size);
    PetResponse updatePet(Long id, PetRequest req, MultipartFile file);
    void deletePet(Long id);

    PetResponse getPetById(Long id);
}
