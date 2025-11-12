package com.raxrot.back.performance.service;

import com.raxrot.back.services.impl.WeatherServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockConstruction;

@ActiveProfiles("test")
@DisplayName("⚡ WeatherService Performance Tests (no external calls)")
class WeatherServicePerformanceTest {

    private static final String WEATHER_JSON = """
        {
          "current": {
            "temperature_2m": 25.2,
            "weathercode": 1
          }
        }
        """;

    private static final String GEO_JSON = """
        {
          "address": {
            "city": "Porto"
          }
        }
        """;

    @Test
    @DisplayName("⏱ First vs second call: cache short-circuits network (RestTemplate constructed once)")
    void firstVsSecondCall_usesCache() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, ctx) -> {
            // Weather API
            org.mockito.Mockito.when(
                    mock.getForObject(startsWith("https://api.open-meteo.com"), eq(String.class))
            ).thenReturn(WEATHER_JSON);

            // Geocoding API
            org.mockito.Mockito.when(
                    mock.exchange(startsWith("https://nominatim.openstreetmap.org"),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(String.class))
            ).thenReturn(ResponseEntity.ok(GEO_JSON));
        })) {
            WeatherServiceImpl service = new WeatherServiceImpl();

            double lat = 41.15;
            double lon = -8.61;

            StopWatch sw = new StopWatch("WeatherService.getWeatherSummary");
            sw.start("first call (miss)");
            String first = service.getWeatherSummary(lat, lon);
            sw.stop();

            sw.start("second call (hit)");
            String second = service.getWeatherSummary(lat, lon);
            sw.stop();

            System.out.println("\n========== 🌦 PERFORMANCE REPORT ==========");
            for (var t : sw.getTaskInfo()) {
                System.out.printf("%s → %d ms%n", t.getTaskName(), t.getTimeMillis());
            }
            System.out.println("Total → " + sw.getTotalTimeMillis() + " ms");
            System.out.println("============================================\n");

            // Same formatted summary
            assertThat(first).isEqualTo(second);
            assertThat(first).contains("Porto");

            // RestTemplate constructed only once -> second call hit the cache
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("🚀 10,000 cached calls: no extra RestTemplate constructions")
    void stressCachedCalls_noExtraNetwork() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, ctx) -> {
            org.mockito.Mockito.when(
                    mock.getForObject(startsWith("https://api.open-meteo.com"), eq(String.class))
            ).thenReturn(WEATHER_JSON);

            org.mockito.Mockito.when(
                    mock.exchange(startsWith("https://nominatim.openstreetmap.org"),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(String.class))
            ).thenReturn(ResponseEntity.ok(GEO_JSON));
        })) {
            WeatherServiceImpl service = new WeatherServiceImpl();

            double lat = 40.0;
            double lon = -7.0;

            // Warm up (fills cache and triggers single construction)
            String baseline = service.getWeatherSummary(lat, lon);
            assertThat(baseline).contains("Porto");
            assertThat(mocked.constructed()).hasSize(1);

            StopWatch sw = new StopWatch("WeatherService.getWeatherSummary cached loop");
            sw.start("10k cached calls");
            for (int i = 0; i < 10_000; i++) {
                String s = service.getWeatherSummary(lat, lon);
                // Quick sanity check every 1000th call
                if ((i % 1000) == 0) assertThat(s).isEqualTo(baseline);
            }
            sw.stop();

            System.out.println("\n========== 🔥 STRESS REPORT (cached) ==========");
            System.out.printf("Total for 10,000 calls: %d ms%n", sw.getTotalTimeMillis());
            System.out.println("===============================================\n");

            // Still exactly one RestTemplate construction
            assertThat(mocked.constructed()).hasSize(1);
        }
    }
}