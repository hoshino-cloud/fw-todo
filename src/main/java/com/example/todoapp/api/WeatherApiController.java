package com.example.todoapp.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherApiController {

    private final WeatherClient weatherClient;

    public WeatherApiController(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @GetMapping("/api/weather")
    public ResponseEntity<WeatherClient.CurrentWeather> weather() {
        WeatherClient.WeatherFetchResult result = weatherClient.getCurrentWeather();
        if (result.unavailable()) {
            return ResponseEntity.ok()
                    .header("X-Weather-Unavailable", "true")
                    .build();
        }
        return ResponseEntity.ok(result.weather());
    }
}
