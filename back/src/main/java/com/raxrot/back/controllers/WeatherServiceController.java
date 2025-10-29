package com.raxrot.back.controllers;

import com.raxrot.back.services.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WeatherServiceController {
    private final WeatherService weatherService;
    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(
            @RequestParam("lat") String latStr,
            @RequestParam("lon") String lonStr) {

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            // Валидация координат
            if (lat < -90 || lat > 90) {
                return ResponseEntity.badRequest().body("Invalid latitude: " + lat);
            }
            if (lon < -180 || lon > 180) {
                return ResponseEntity.badRequest().body("Invalid longitude: " + lon);
            }

            String weather = weatherService.getWeatherSummary(lat, lon);
            return ResponseEntity.ok().body(Map.of("weather", weather));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid coordinates format");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching weather");
        }
    }
}