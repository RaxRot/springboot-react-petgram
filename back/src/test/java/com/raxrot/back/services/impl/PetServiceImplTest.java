package com.raxrot.back.services.impl;

import com.raxrot.back.dtos.PetPageResponse;
import com.raxrot.back.dtos.PetRequest;
import com.raxrot.back.dtos.PetResponse;
import com.raxrot.back.enums.PetType;
import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.models.Pet;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.PetRepository;
import com.raxrot.back.services.FileUploadService;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("PetServiceImpl Tests")
class PetServiceImplTest {

    @Mock private PetRepository petRepository;
    @Mock private AuthUtil authUtil;
    @Mock private ModelMapper modelMapper;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks
    private PetServiceImpl petService;

    private User me;
    private Pet pet;
    private PetRequest req;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        me = new User();
        me.setUserId(1L);
        me.setUserName("vlad");
        me.setProfilePic("pic.png");

        pet = new Pet();
        pet.setId(10L);
        pet.setName("Rex");
        pet.setType(PetType.DOG);
        pet.setOwner(me);
        pet.setPhotoUrl("old_photo.jpg");

        req = new PetRequest();
        req.setName("Rex");
        req.setType(PetType.DOG);
        req.setAge(3);
        req.setDescription("Friendly dog");

        file = mock(MultipartFile.class);

        lenient().when(modelMapper.map(any(PetRequest.class), eq(Pet.class))).thenReturn(pet);
        lenient().when(modelMapper.map(any(Pet.class), eq(PetResponse.class)))
                .thenReturn(new PetResponse(10L, "Rex", PetType.DOG, null, 3,
                        "Friendly dog", "photo.jpg", "vlad", "pic.png"));
    }

    @Test
    void should_create_pet_successfully_without_photo() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.save(any(Pet.class))).willReturn(pet);

        PetResponse resp = petService.createPet(req, null);

        assertThat(resp.getName()).isEqualTo("Rex");
        verify(petRepository).save(any(Pet.class));
        verify(fileUploadService, never()).uploadFile(any());
    }

    @Test
    void should_create_pet_with_photo_upload() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(file.isEmpty()).willReturn(false);
        given(fileUploadService.uploadFile(file)).willReturn("new_photo.jpg");
        given(petRepository.save(any(Pet.class))).willReturn(pet);
        given(modelMapper.map(any(Pet.class), eq(PetResponse.class)))
                .willReturn(new PetResponse(10L, "Rex", PetType.DOG, null, 3,
                        "Friendly dog", "new_photo.jpg", "vlad", "pic.png"));

        PetResponse resp = petService.createPet(req, file);

        assertThat(resp.getPhotoUrl()).isEqualTo("new_photo.jpg");
        verify(fileUploadService).uploadFile(file);
    }

    @Test
    void should_get_my_pets() {
        given(authUtil.loggedInUser()).willReturn(me);
        Page<Pet> petPage = new PageImpl<>(List.of(pet));
        given(petRepository.findAllByOwner_UserId(eq(1L), any(Pageable.class))).willReturn(petPage);

        PetPageResponse resp = petService.getMyPets(0, 5);

        assertThat(resp.getContent()).hasSize(1);
    }

    @Test
    void should_get_pets_by_username() {
        Page<Pet> petPage = new PageImpl<>(List.of(pet));
        given(petRepository.findAllByOwner_UserName(eq("vlad"), any(Pageable.class))).willReturn(petPage);

        PetPageResponse resp = petService.getPetsByUsername("vlad", 0, 5);

        assertThat(resp.getContent()).hasSize(1);
    }

    @Test
    void should_update_pet_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));
        given(petRepository.save(any(Pet.class))).willReturn(pet);
        lenient().when(modelMapper.map(any(Pet.class), eq(PetResponse.class)))
                .thenReturn(new PetResponse(10L, "Rex Updated", PetType.DOG, null, 4, "Updated dog", null, "vlad", "pic.png"));

        PetResponse resp = petService.updatePet(10L, req, null);

        assertThat(resp.getName()).isEqualTo("Rex Updated");
    }

    @Test
    void should_throw_when_updating_non_existing_pet() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> petService.updatePet(10L, req, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    void should_throw_when_editing_others_pet() {
        User other = new User();
        other.setUserId(99L);
        pet.setOwner(other);

        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));

        assertThatThrownBy(() -> petService.updatePet(10L, req, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot edit this pet");
    }

    @Test
    void should_update_pet_with_new_photo() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));
        given(file.isEmpty()).willReturn(false);
        given(fileUploadService.uploadFile(file)).willReturn("new_photo.jpg");
        given(petRepository.save(any(Pet.class))).willReturn(pet);
        lenient().when(modelMapper.map(any(Pet.class), eq(PetResponse.class)))
                .thenReturn(new PetResponse(10L, "Rex", PetType.DOG, null, 3,
                        "Friendly dog", "new_photo.jpg", "vlad", "pic.png"));

        PetResponse resp = petService.updatePet(10L, req, file);

        assertThat(resp.getPhotoUrl()).isEqualTo("new_photo.jpg");
        verify(fileUploadService).deleteFile("old_photo.jpg");
    }

    @Test
    void should_delete_pet_successfully() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));

        petService.deletePet(10L);

        verify(petRepository).delete(pet);
    }

    @Test
    void should_delete_pet_with_photo() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));

        petService.deletePet(10L);

        verify(fileUploadService).deleteFile("old_photo.jpg");
    }

    @Test
    void should_throw_when_deleting_others_pet() {
        User other = new User();
        other.setUserId(99L);
        pet.setOwner(other);

        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));

        assertThatThrownBy(() -> petService.deletePet(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot delete this pet");
    }

    @Test
    void should_throw_when_deleting_non_existing_pet() {
        given(authUtil.loggedInUser()).willReturn(me);
        given(petRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> petService.deletePet(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    void should_get_pet_by_id() {
        given(petRepository.findById(10L)).willReturn(Optional.of(pet));
        given(modelMapper.map(any(Pet.class), eq(PetResponse.class)))
                .willReturn(new PetResponse(10L, "Rex", PetType.DOG, null, 3, "Friendly dog", null, "vlad", "pic.png"));

        PetResponse resp = petService.getPetById(10L);

        assertThat(resp.getName()).isEqualTo("Rex");
    }

    @Test
    void should_throw_when_pet_not_found_by_id() {
        given(petRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getPetById(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Pet not found");
    }
}