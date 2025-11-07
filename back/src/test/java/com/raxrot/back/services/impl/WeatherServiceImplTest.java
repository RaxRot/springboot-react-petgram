package com.raxrot.back.services.impl;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("WeatherServiceImpl Tests")
class WeatherServiceImplTest {

    private final WeatherServiceImpl weatherService = new WeatherServiceImpl();

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should fetch weather, build summary and cache it")
    void should_fetch_weather_and_cache_it() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (restMock, context) -> {
                    // Weather API mock
                    JSONObject weatherJson = new JSONObject("""
                        {"current": {"temperature_2m": 21.5, "weathercode": 1}}
                        """);
                    when(restMock.getForObject(anyString(), eq(String.class)))
                            .thenReturn(weatherJson.toString());

                    // Geo API mock
                    JSONObject geoJson = new JSONObject("""
                        {"address": {"city": "Porto"}}
                        """);
                    when(restMock.exchange(anyString(), any(), any(), eq(String.class)))
                            .thenReturn(ResponseEntity.ok(geoJson.toString()));
                })) {

            String result = weatherService.getWeatherSummary(41.15, -8.61);

            assertThat(result).contains("🌤️");
            assertThat(result).containsAnyOf("21.5", "21,5");
            assertThat(result).contains("Porto");

            Map<?, ?> cache = weatherService.getCache();
            assertThat(cache).hasSize(1);

            String cached = weatherService.getWeatherSummary(41.15, -8.61);
            assertThat(cached).isEqualTo(result);

            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should return Unknown when API returns no 'current'")
    void should_return_unknown_if_no_current_field() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (restMock, context) -> {
                    when(restMock.getForObject(anyString(), eq(String.class)))
                            .thenReturn("{}"); // No "current"
                })) {

            String result = weatherService.getWeatherSummary(52.00, 13.00);

            assertThat(result).isEqualTo("☁️ Unknown");
        }
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should return Unknown when external API request fails")
    void should_return_unknown_on_api_error() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (restMock, context) -> {
                    when(restMock.getForObject(anyString(), eq(String.class)))
                            .thenThrow(new RuntimeException("API error"));
                })) {

            String result = weatherService.getWeatherSummary(10.00, 20.00);

            assertThat(result).isEqualTo("☁️ Unknown");
        }
    }

    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should clean up expired cache entries")
    void should_cleanup_expired_cache() {
        // Given: add entries directly
        Map<String, WeatherServiceImpl.CacheEntry> cache = weatherService.getCache();

        cache.put("10.00,20.00",
                new WeatherServiceImpl.CacheEntry("old", Instant.now().minusSeconds(5)));

        cache.put("30.00,40.00",
                new WeatherServiceImpl.CacheEntry("ok", Instant.now().plusSeconds(60)));

        // When
        weatherService.cleanupCache();

        // Then
        assertThat(cache).hasSize(1);
        assertThat(cache.values().toString()).contains("ok");
    }

}
