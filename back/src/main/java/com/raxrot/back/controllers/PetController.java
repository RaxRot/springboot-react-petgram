package com.raxrot.back.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PetController {

    private final PetService petService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> createPet(
            @RequestPart("data") String requestString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            PetRequest petRequest = objectMapper.readValue(requestString, PetRequest.class);
            PetResponse response = petService.createPet(petRequest, file);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            throw new ApiException("Invalid JSON for pet data", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/pets")
    public ResponseEntity<PetPageResponse> getMyPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(petService.getMyPets(page, size));
    }

    @GetMapping("/public/users/{username}/pets")
    public ResponseEntity<PetPageResponse> getUserPets(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(petService.getPetsByUsername(username, page, size));
    }

    @PatchMapping(value = "/pets/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long id,
            @RequestPart("data") String requestString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            PetRequest petRequest = objectMapper.readValue(requestString, PetRequest.class);
            PetResponse response = petService.updatePet(id, petRequest, file);
            return ResponseEntity.ok(response);
        } catch (JsonProcessingException e) {
            throw new ApiException("Invalid JSON for pet data", HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/pets/{id}")
    public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }
}
