package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolidayApiController {

    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping("/api/holidays")
    public ResponseEntity<Map<String, String>> holidays(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        HolidayClient.HolidayFetchResult result = holidayClient.getHolidays();
        Map<String, String> filteredHolidays = result.holidays().entrySet().stream()
                .filter(holiday -> isInRange(LocalDate.parse(holiday.getKey()), from, to))
                .collect(LinkedHashMap::new,
                        (holidays, holiday) -> holidays.put(holiday.getKey(), holiday.getValue()),
                        LinkedHashMap::putAll);
        if (result.unavailable()) {
            return ResponseEntity.ok()
                    .header("X-Holidays-Unavailable", "true")
                    .body(filteredHolidays);
        }
        return ResponseEntity.ok(filteredHolidays);
    }

    private boolean isInRange(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }
}
