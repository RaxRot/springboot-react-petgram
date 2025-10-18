package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Pet;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.services.PetService;
import com.raxrot.back.utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;
    private final FileUploadService fileUploadService;

    @Transactional
    @Override
    public PetResponse createPet(PetRequest req, MultipartFile file) {
        User me = authUtil.loggedInUser();
        Pet pet = modelMapper.map(req, Pet.class);
        pet.setOwner(me);

        if (file != null && !file.isEmpty()) {
            String photoUrl = fileUploadService.uploadFile(file);
            pet.setPhotoUrl(photoUrl);
        }

        Pet saved = petRepository.save(pet);
        PetResponse resp = modelMapper.map(saved, PetResponse.class);
        resp.setOwnerUsername(me.getUserName());
        resp.setOwnerProfilePic(me.getProfilePic());
        return resp;
    }

    @Override
    public PetPageResponse getMyPets(int page, int size) {
        User me = authUtil.loggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Pet> petPage = petRepository.findAllByOwner_UserId(me.getUserId(), pageable);

        List<PetResponse> content = petPage.getContent().stream()
                .map(p -> modelMapper.map(p, PetResponse.class))
                .toList();

        return new PetPageResponse(content, petPage.getNumber(), petPage.getSize(),
                petPage.getTotalPages(), petPage.getTotalElements(), petPage.isLast());
    }

    @Override
    public PetPageResponse getPetsByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Pet> petPage = petRepository.findAllByOwner_UserName(username, pageable);

        List<PetResponse> content = petPage.getContent().stream()
                .map(p -> modelMapper.map(p, PetResponse.class))
                .toList();

        return new PetPageResponse(content, petPage.getNumber(), petPage.getSize(),
                petPage.getTotalPages(), petPage.getTotalElements(), petPage.isLast());
    }

    @Transactional
    @Override
    public PetResponse updatePet(Long id, PetRequest req, MultipartFile file) {
        User me = authUtil.loggedInUser();
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ApiException("Pet not found", HttpStatus.NOT_FOUND));

        if (!pet.getOwner().getUserId().equals(me.getUserId())) {
            throw new ApiException("You cannot edit this pet", HttpStatus.FORBIDDEN);
        }

        modelMapper.map(req, pet);

        if (file != null && !file.isEmpty()) {
            String newPhoto = fileUploadService.uploadFile(file);
            if (pet.getPhotoUrl() != null) {
                try {
                    fileUploadService.deleteFile(pet.getPhotoUrl());
                } catch (Exception ignored) {}
            }
            pet.setPhotoUrl(newPhoto);
        }

        Pet updated = petRepository.save(pet);
        return modelMapper.map(updated, PetResponse.class);
    }

    @Transactional
    @Override
    public void deletePet(Long id) {
        User me = authUtil.loggedInUser();
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ApiException("Pet not found", HttpStatus.NOT_FOUND));

        if (!pet.getOwner().getUserId().equals(me.getUserId())) {
            throw new ApiException("You cannot delete this pet", HttpStatus.FORBIDDEN);
        }

        if (pet.getPhotoUrl() != null) {
            try {
                fileUploadService.deleteFile(pet.getPhotoUrl());
            } catch (Exception ignored) {}
        }

        petRepository.delete(pet);
    }

    @Override
    public PetResponse getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ApiException("Pet not found", HttpStatus.NOT_FOUND));
        return modelMapper.map(pet, PetResponse.class);
    }

}
