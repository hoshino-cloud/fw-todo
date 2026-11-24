package com.example.todoapp.api;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class HolidayClient {

    private final RestClient restClient;

    public HolidayClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public HolidayFetchResult getHolidays() {
        try {
            Map<String, String> holidays = restClient.get()
                    .uri("https://holidays-jp.github.io/api/v1/date.json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            return new HolidayFetchResult(holidays == null ? Collections.emptyMap() : holidays, false);
        } catch (RestClientException exception) {
            return new HolidayFetchResult(Collections.emptyMap(), true);
        }
    }

    public record HolidayFetchResult(Map<String, String> holidays, boolean unavailable) {
    }
}
