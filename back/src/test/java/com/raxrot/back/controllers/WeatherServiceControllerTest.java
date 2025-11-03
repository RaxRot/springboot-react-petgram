package com.raxrot.back.controllers;

import com.raxrot.back.security.jwt.AuthTokenFilter;
import com.raxrot.back.security.jwt.JwtUtils;
import com.raxrot.back.security.services.UserDetailsServiceImpl;
import com.raxrot.back.services.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WeatherServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WeatherServiceControllerTest {

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private WeatherService weatherService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/weather — should return weather summary when valid coordinates")
    void getWeather_ShouldReturnWeatherSummary() throws Exception {
        Mockito.when(weatherService.getWeatherSummary(41.15, -8.61))
                .thenReturn("☀️ 25°C • Porto");

        mockMvc.perform(get("/api/weather")
                        .param("lat", "41.15")
                        .param("lon", "-8.61")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weather").value("☀️ 25°C • Porto"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 400 for invalid format")
    void getWeather_ShouldReturnBadRequest_WhenInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("lat", "abc")
                        .param("lon", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid coordinates format"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 400 when latitude < -90")
    void getWeather_ShouldRejectLatitudeBelowMin() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("lat", "-91")
                        .param("lon", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid latitude: -91.0"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 400 when latitude > 90")
    void getWeather_ShouldRejectLatitudeAboveMax() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("lat", "95")
                        .param("lon", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid latitude: 95.0"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 400 when longitude < -180")
    void getWeather_ShouldRejectLongitudeBelowMin() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("lat", "10")
                        .param("lon", "-181"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid longitude: -181.0"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 400 when longitude > 180")
    void getWeather_ShouldRejectLongitudeAboveMax() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("lat", "10")
                        .param("lon", "181"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid longitude: 181.0"));
    }

    @Test
    @DisplayName("GET /api/weather — should return 500 when service throws exception")
    void getWeather_ShouldReturnInternalServerError() throws Exception {
        Mockito.when(weatherService.getWeatherSummary(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Boom"));

        mockMvc.perform(get("/api/weather")
                        .param("lat", "10")
                        .param("lon", "20"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error fetching weather"));
    }
}
