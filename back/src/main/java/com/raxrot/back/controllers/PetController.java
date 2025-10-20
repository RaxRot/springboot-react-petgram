package com.raxrot.back.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Pets",
        description = "Endpoints for managing pets — create, update, delete, and fetch pet information."
)
public class PetController {

    private final PetService petService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "Create a new pet",
            description = "Creates a new pet entry for the authenticated user. Accepts multipart/form-data with pet JSON and optional image file.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pet created successfully",
                            content = @Content(schema = @Schema(implementation = PetResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON or missing data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> createPet(
            @Parameter(description = "Pet data as JSON string") @RequestPart("data") String requestString,
            @Parameter(description = "Optional image file of the pet") @RequestPart(value = "file", required = false) MultipartFile file
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

    @Operation(
            summary = "Get current user's pets",
            description = "Retrieves all pets belonging to the currently authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User's pets retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PetPageResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT")
            }
    )
    @GetMapping("/user/pets")
    public ResponseEntity<PetPageResponse> getMyPets(
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📄 Fetching current user's pets | page={}, size={}", page, size);
        PetPageResponse response = petService.getMyPets(page, size);
        log.info("✅ Retrieved {} pets for current user", response.getContent().size());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get pets by username",
            description = "Returns a list of public pets belonging to the specified user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pets retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PetPageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/public/users/{username}/pets")
    public ResponseEntity<PetPageResponse> getUserPets(
            @Parameter(description = "Username of the pet owner") @PathVariable String username,
            @Parameter(description = "Page number (default = 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default = 10)") @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📄 Fetching pets for username='{}' | page={}, size={}", username, page, size);
        PetPageResponse response = petService.getPetsByUsername(username, page, size);
        log.info("✅ Retrieved {} pets for user '{}'", response.getContent().size(), username);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update pet",
            description = "Updates an existing pet record. Accepts multipart/form-data with updated JSON and optional new image.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pet updated successfully",
                            content = @Content(schema = @Schema(implementation = PetResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON or data format"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Pet not found")
            }
    )
    @PatchMapping(value = "/pets/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponse> updatePet(
            @Parameter(description = "ID of the pet to update") @PathVariable Long id,
            @Parameter(description = "Updated pet data as JSON string") @RequestPart("data") String requestString,
            @Parameter(description = "Optional new image file") @RequestPart(value = "file", required = false) MultipartFile file
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

    @Operation(
            summary = "Delete pet",
            description = "Deletes an existing pet. Only the pet owner can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Pet deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
                    @ApiResponse(responseCode = "404", description = "Pet not found")
            }
    )
    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(
            @Parameter(description = "ID of the pet to delete") @PathVariable Long id
    ) {
        log.info("🗑️ Delete pet request received | petId={}", id);
        petService.deletePet(id);
        log.info("✅ Pet deleted successfully | petId={}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get pet by ID",
            description = "Fetches detailed information about a specific pet (public endpoint).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pet details retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PetResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pet not found")
            }
    )
    @GetMapping("/public/pets/{id}")
    public ResponseEntity<PetResponse> getPetById(
            @Parameter(description = "ID of the pet to fetch") @PathVariable Long id
    ) {
        log.info("🔎 Fetching pet details | petId={}", id);
        PetResponse response = petService.getPetById(id);
        log.info("✅ Pet details retrieved successfully | petId={}", id);
        return ResponseEntity.ok(response);
    }
}
