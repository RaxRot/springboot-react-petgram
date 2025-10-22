package com.raxrot.back.repositories;

import com.raxrot.back.enums.PetType;
import com.raxrot.back.models.Pet;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PetRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner1;
    private User owner2;

    @BeforeEach
    void setUp() {
        owner1 = new User("alice", "alice@example.com", "password");
        owner2 = new User("bob", "bob@example.com", "password");
        userRepository.save(owner1);
        userRepository.save(owner2);

        Pet pet1 = new Pet();
        pet1.setName("Buddy");
        pet1.setType(PetType.DOG);
        pet1.setBreed("Labrador");
        pet1.setAge(3);
        pet1.setOwner(owner1);

        Pet pet2 = new Pet();
        pet2.setName("Milo");
        pet2.setType(PetType.CAT);
        pet2.setBreed("Siamese");
        pet2.setAge(2);
        pet2.setOwner(owner1);

        Pet pet3 = new Pet();
        pet3.setName("Polly");
        pet3.setType(PetType.PARROT);
        pet3.setBreed("Macaw");
        pet3.setAge(1);
        pet3.setOwner(owner2);

        petRepository.save(pet1);
        petRepository.save(pet2);
        petRepository.save(pet3);
    }

    @Test
    @DisplayName("findAllByOwner_UserId should return pets belonging to the given userId")
    void findAllByOwner_UserId_ShouldReturnCorrectPets() {
        Page<Pet> result = petRepository.findAllByOwner_UserId(owner1.getUserId(), PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).allMatch(pet -> pet.getOwner().getUserId().equals(owner1.getUserId()));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllByOwner_UserName should return pets belonging to the given username")
    void findAllByOwner_UserName_ShouldReturnCorrectPets() {
        Page<Pet> result = petRepository.findAllByOwner_UserName("bob", PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).allMatch(pet -> pet.getOwner().getUserName().equals("bob"));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("countByOwner_UserId should return correct pet count for each user")
    void countByOwner_UserId_ShouldReturnCorrectCount() {
        long countOwner1 = petRepository.countByOwner_UserId(owner1.getUserId());
        long countOwner2 = petRepository.countByOwner_UserId(owner2.getUserId());

        assertThat(countOwner1).isEqualTo(2);
        assertThat(countOwner2).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllByOwner_UserId should return empty when user has no pets")
    void findAllByOwner_UserId_ShouldReturnEmptyForNoPets() {
        User newUser = new User("charlie", "charlie@example.com", "password");
        userRepository.save(newUser);

        Page<Pet> result = petRepository.findAllByOwner_UserId(newUser.getUserId(), PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}
