package com.raxrot.back.services.impl;

import com.raxrot.back.services.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String summary, Instant expiresAt) {}

    @Override
    public String getWeatherSummary(double lat, double lon) {
        String key = String.format(Locale.US, "%.2f,%.2f", lat, lon);

        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt().isAfter(Instant.now())) {
            return entry.summary();
        }

        RestTemplate rest = new RestTemplate();
        String summary = "☁️ Unknown";

        try {
            // ✅ Правильное форматирование координат
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat df = new DecimalFormat("0.####", symbols);

            String latStr = df.format(lat);
            String lonStr = df.format(lon);

            // 🧭 1️⃣ Получаем погоду
            String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + latStr
                    + "&longitude=" + lonStr
                    + "&current=temperature_2m,weathercode&timezone=auto";

            log.info("🌍 Fetching weather for lat={}, lon={} | URL={}", latStr, lonStr, weatherUrl);

            String weatherJson = rest.getForObject(weatherUrl, String.class);
            JSONObject json = new JSONObject(weatherJson);

            if (!json.has("current")) {
                log.warn("⚠️ Weather API returned no 'current' field");
                return "☁️ Unknown";
            }

            JSONObject current = json.getJSONObject("current");
            double temp = current.optDouble("temperature_2m", Double.NaN);
            int code = current.optInt("weathercode", 0);
            String icon = mapWeather(code);

            // 🏙 2️⃣ Получаем город через OpenStreetMap Nominatim
            String city = "Unknown";
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.add("User-Agent", "PetgramWeatherBot/1.0");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String geoUrl = "https://nominatim.openstreetmap.org/reverse"
                        + "?lat=" + latStr
                        + "&lon=" + lonStr
                        + "&format=json";

                ResponseEntity<String> response = rest.exchange(geoUrl, HttpMethod.GET, entity, String.class);
                JSONObject geo = new JSONObject(response.getBody());

                if (geo.has("address")) {
                    JSONObject addr = geo.getJSONObject("address");
                    city = addr.optString("city",
                            addr.optString("town",
                                    addr.optString("village",
                                            addr.optString("municipality", "Unknown"))));
                }
            } catch (Exception e) {
                log.warn("⚠️ Geocoding failed: {}", e.getMessage());
            }

            // 📦 3️⃣ Формируем результат
            if (!Double.isNaN(temp))
                summary = String.format("%s %.1f°C • %s", icon, temp, city);
            else
                summary = String.format("%s • %s", icon, city);

            cache.put(key, new CacheEntry(summary, Instant.now().plusSeconds(1800)));
            log.info("✅ Weather updated for {}: {}", key, summary);

        } catch (Exception e) {
            log.error("❌ Weather fetch failed: {}", e.getMessage());
        }

        return summary;
    }

    private String mapWeather(int code) {
        return switch (code) {
            case 0 -> "☀️";
            case 1, 2, 3 -> "🌤️";
            case 45, 48 -> "🌫️";
            case 51, 53, 55, 56, 57 -> "🌦️";
            case 61, 63, 65, 80, 81, 82 -> "🌧️";
            case 66, 67, 71, 73, 75, 77, 85, 86 -> "❄️";
            case 95, 96, 99 -> "⛈️";
            default -> "☁️";
        };
    }

    @Scheduled(fixedRate = 7_200_000)
    public void cleanupCache() {
        cache.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(Instant.now()));
    }
}
