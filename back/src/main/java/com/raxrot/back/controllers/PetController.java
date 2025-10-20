package com.raxrot.back.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class PetController {

    private final PetService petService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> createPet(
            @RequestPart("data") String requestString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("🐾 Create pet request received | hasFile={}", file != null);
        try {
            PetRequest petRequest = objectMapper.readValue(requestString, PetRequest.class);
            log.info("📦 Parsed pet data successfully for name='{}'", petRequest.getName());
            PetResponse response = petService.createPet(petRequest, file);
            log.info("✅ Pet created successfully | petId={}", response.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse pet JSON data: {}", e.getMessage());
            throw new ApiException("Invalid JSON for pet data", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/pets")
    public ResponseEntity<PetPageResponse> getMyPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📄 Fetching current user's pets | page={}, size={}", page, size);
        PetPageResponse response = petService.getMyPets(page, size);
        log.info("✅ Retrieved {} pets for current user", response.getContent().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/users/{username}/pets")
    public ResponseEntity<PetPageResponse> getUserPets(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📄 Fetching pets for username='{}' | page={}, size={}", username, page, size);
        PetPageResponse response = petService.getPetsByUsername(username, page, size);
        log.info("✅ Retrieved {} pets for user '{}'", response.getContent().size(), username);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/pets/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long id,
            @RequestPart("data") String requestString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("✏️ Update pet request received | petId={} | hasFile={}", id, file != null);
        try {
            PetRequest petRequest = objectMapper.readValue(requestString, PetRequest.class);
            log.info("📦 Parsed update data for petId={} | name='{}'", id, petRequest.getName());
            PetResponse response = petService.updatePet(id, petRequest, file);
            log.info("✅ Pet updated successfully | petId={}", id);
            return ResponseEntity.ok(response);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse update pet JSON data for petId={}: {}", id, e.getMessage());
            throw new ApiException("Invalid JSON for pet data", HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        log.info("🗑️ Delete pet request received | petId={}", id);
        petService.deletePet(id);
        log.info("✅ Pet deleted successfully | petId={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/pets/{id}")
    public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
        log.info("🔎 Fetching pet details | petId={}", id);
        PetResponse response = petService.getPetById(id);
        log.info("✅ Pet details retrieved successfully | petId={}", id);
        return ResponseEntity.ok(response);
    }
}
