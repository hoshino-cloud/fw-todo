package com.example.todoapp.api;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WeatherClient {

    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=35.6895&longitude=139.6917&current=temperature_2m,weather_code&timezone=Asia/Tokyo";
    private static final Pattern CURRENT_WEATHER_PATTERN = Pattern.compile(
            "\\\"current\\\"\\s*:\\s*\\{\\s*\\\"time\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"interval\\\"\\s*:\\s*\\d+\\s*,\\s*\\\"temperature_2m\\\"\\s*:\\s*"
                    + "(-?\\d+(?:\\.\\d+)?)\\s*,\\s*\\\"weather_code\\\"\\s*:\\s*(\\d+)");

    private final RestClient restClient;

    public WeatherClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public WeatherFetchResult getCurrentWeather() {
        try {
            String response = restClient.get()
                    .uri(WEATHER_URL)
                    .retrieve()
                    .body(String.class);
            if (response == null) {
                return new WeatherFetchResult(null, true);
            }
            Matcher matcher = CURRENT_WEATHER_PATTERN.matcher(response);
            if (!matcher.find()) {
                return new WeatherFetchResult(null, true);
            }
            return new WeatherFetchResult(new CurrentWeather(
                    matcher.group(1), Double.valueOf(matcher.group(2)), Integer.valueOf(matcher.group(3))), false);
        } catch (RestClientException exception) {
            return new WeatherFetchResult(null, true);
        }
    }

    public record CurrentWeather(String time, Double temperature, Integer weatherCode) {
    }

    public record WeatherFetchResult(CurrentWeather weather, boolean unavailable) {
    }
}
