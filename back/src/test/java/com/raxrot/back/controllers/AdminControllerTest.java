package com.raxrot.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxrot.back.dtos.DonationResponse;
import com.raxrot.back.dtos.UserPageResponse;
import com.raxrot.back.dtos.UserResponse;
import com.raxrot.back.services.AdminService;
import com.raxrot.back.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {


    @MockBean
    private com.raxrot.back.security.jwt.JwtUtils jwtUtils;

    @MockBean
    private com.raxrot.back.security.jwt.AuthTokenFilter authTokenFilter;

    @MockBean
    private com.raxrot.back.security.services.UserDetailsServiceImpl userDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private AdminService adminService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse userResponse;
    private UserPageResponse userPageResponse;
    private DonationResponse donationResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(1L, "alice", "alice@example.com", "pic.jpg", false);
        userPageResponse = new UserPageResponse(List.of(userResponse), 0, 10, 1, 1L, true);
        donationResponse = new DonationResponse(1L, "alice", "bob", 100L, "USD", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/admin/users — should return paginated users")
    void getAllUsers_ShouldReturnUserPageResponse() throws Exception {
        Mockito.when(userService.getAllUsers(any(), any(), any(), any()))
                .thenReturn(userPageResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "userName")
                        .param("sortOrder", "asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userName").value("alice"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService, times(1)).getAllUsers(0, 10, "userName", "asc");
    }

    @Test
    @DisplayName("GET /api/admin/users/id/{id} — should return user by ID")
    void getUserById_ShouldReturnUserResponse() throws Exception {
        Mockito.when(userService.getUserById(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/id/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("GET /api/admin/users/email/{email} — should return user by email")
    void getUserByEmail_ShouldReturnUserResponse() throws Exception {
        Mockito.when(userService.getUserByEmail("alice@example.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/email/{email}", "alice@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(userService, times(1)).getUserByEmail("alice@example.com");
    }

    @Test
    @DisplayName("PATCH /api/admin/users/ban/{id} — should ban user")
    void banUser_ShouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/ban/{id}", 1L))
                .andExpect(status().isOk());

        verify(userService, times(1)).banUser(1L);
    }

    @Test
    @DisplayName("PATCH /api/admin/users/unban/{id} — should unban user")
    void unBanUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/admin/users/unban/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).unbanUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} — should delete user")
    void deleteUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUserById(1L);
    }

    @Test
    @DisplayName("GET /api/admin/users/donations — should return donations list")
    void getAllDonations_ShouldReturnDonationList() throws Exception {
        Mockito.when(adminService.getAllDonations()).thenReturn(List.of(donationResponse));

        mockMvc.perform(get("/api/admin/users/donations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donorUsername").value("alice"))
                .andExpect(jsonPath("$[0].receiverUsername").value("bob"))
                .andExpect(jsonPath("$[0].currency").value("USD"));

        verify(adminService, times(1)).getAllDonations();
    }

    @Test
    @DisplayName("GET /api/admin/users/stats — should return platform statistics")
    void getAdminStats_ShouldReturnStatsMap() throws Exception {
        Mockito.when(adminService.countUsers()).thenReturn(10L);
        Mockito.when(adminService.countDonations()).thenReturn(5L);
        Mockito.when(adminService.countComments()).thenReturn(20L);
        Mockito.when(adminService.countPosts()).thenReturn(15L);

        mockMvc.perform(get("/api/admin/users/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(10))
                .andExpect(jsonPath("$.donations").value(5))
                .andExpect(jsonPath("$.comments").value(20))
                .andExpect(jsonPath("$.posts").value(15));

        verify(adminService, times(1)).countUsers();
        verify(adminService, times(1)).countDonations();
        verify(adminService, times(1)).countComments();
        verify(adminService, times(1)).countPosts();
    }
}
