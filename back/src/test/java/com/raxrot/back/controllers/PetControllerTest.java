package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.enums.PetType;
import com.raxrot.back.services.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PetControllerTest {

    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PetService petService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PetResponse petResponse;
    private PetPageResponse petPageResponse;
    private PetRequest petRequest;

    @BeforeEach
    void setup() {
        petResponse = new PetResponse(1L, "Luna", PetType.DOG, "Husky", 3, "Friendly dog", "photo.jpg", "alice", "pic.jpg");
        petPageResponse = new PetPageResponse(List.of(petResponse), 0, 10, 1, 1L, true);
        petRequest = new PetRequest();
        petRequest.setName("Luna");
        petRequest.setType(PetType.DOG);
        petRequest.setBreed("Husky");
        petRequest.setAge(3);
        petRequest.setDescription("Friendly dog");
    }

    @Test
    @DisplayName("POST /api/pets — should create pet")
    void createPet_ShouldReturnCreatedPet() throws Exception {
        MockMultipartFile jsonPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(petRequest));
        MockMultipartFile filePart = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fakeimage".getBytes());
        Mockito.when(petService.createPet(any(PetRequest.class), any())).thenReturn(petResponse);

        mockMvc.perform(multipart("/api/pets")
                        .file(jsonPart)
                        .file(filePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.type").value("DOG"));

        verify(petService, times(1)).createPet(any(PetRequest.class), any());
    }

    @Test
    @DisplayName("GET /api/user/pets — should return user's pets")
    void getMyPets_ShouldReturnPetPage() throws Exception {
        Mockito.when(petService.getMyPets(anyInt(), anyInt())).thenReturn(petPageResponse);

        mockMvc.perform(get("/api/user/pets")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Luna"));

        verify(petService, times(1)).getMyPets(0, 10);
    }

    @Test
    @DisplayName("GET /api/public/users/{username}/pets — should return user's public pets")
    void getUserPets_ShouldReturnPage() throws Exception {
        Mockito.when(petService.getPetsByUsername(anyString(), anyInt(), anyInt())).thenReturn(petPageResponse);

        mockMvc.perform(get("/api/public/users/{username}/pets", "alice")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Luna"));

        verify(petService, times(1)).getPetsByUsername("alice", 0, 10);
    }

    @Test
    @DisplayName("PATCH /api/pets/{id} — should update pet")
    void updatePet_ShouldReturnUpdatedPet() throws Exception {
        MockMultipartFile jsonPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(petRequest));
        Mockito.when(petService.updatePet(anyLong(), any(PetRequest.class), any())).thenReturn(petResponse);

        mockMvc.perform(multipart("/api/pets/{id}", 1L)
                        .file(jsonPart)
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.type").value("DOG"));

        verify(petService, times(1)).updatePet(anyLong(), any(PetRequest.class), any());
    }

    @Test
    @DisplayName("DELETE /api/pets/{id} — should delete pet")
    void deletePet_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/pets/{id}", 1L))
                .andExpect(status().isNoContent());
        verify(petService, times(1)).deletePet(1L);
    }

    @Test
    @DisplayName("GET /api/public/pets/{id} — should return pet details")
    void getPetById_ShouldReturnPet() throws Exception {
        Mockito.when(petService.getPetById(anyLong())).thenReturn(petResponse);

        mockMvc.perform(get("/api/public/pets/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.breed").value("Husky"));

        verify(petService, times(1)).getPetById(1L);
    }
}
