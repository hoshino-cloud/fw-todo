package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) LocalDate date,
            Model model) {
        LocalDate today = LocalDate.now();
        YearMonth displayedMonth = YearMonth.of(
                year != null ? year : today.getYear(),
                month != null ? month : today.getMonthValue());

        boolean weekView = "week".equals(view);
        LocalDate selectedDate = date != null ? date : displayedMonth.atDay(1);
        LocalDate firstDay = weekView
                ? selectedDate.minusDays(selectedDate.getDayOfWeek().getValue() % 7)
                : displayedMonth.atDay(1);
        LocalDate lastDay = weekView ? firstDay.plusDays(6) : displayedMonth.atEndOfMonth();

        model.addAttribute("displayedMonth", displayedMonth);
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("weeks", weekView
                ? Collections.singletonList(createWeek(firstDay))
                : createWeeks(firstDay, lastDay));
        model.addAttribute("weekView", weekView);
        model.addAttribute("previousWeek", firstDay.minusWeeks(1));
        model.addAttribute("nextWeek", firstDay.plusWeeks(1));
        model.addAttribute("previousMonth", displayedMonth.minusMonths(1));
        model.addAttribute("nextMonth", displayedMonth.plusMonths(1));
        return "calendar";
    }

    private List<LocalDate> createWeek(LocalDate firstDay) {
        List<LocalDate> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(firstDay.plusDays(i));
        }
        return week;
    }

    private List<List<LocalDate>> createWeeks(LocalDate firstDay, LocalDate lastDay) {
        List<List<LocalDate>> weeks = new ArrayList<>();
        List<LocalDate> week = new ArrayList<>();

        for (int i = 0; i < firstDay.getDayOfWeek().getValue() % 7; i++) {
            week.add(null);
        }

        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            week.add(day);
            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }

        if (!week.isEmpty()) {
            while (week.size() < 7) {
                week.add(null);
            }
            weeks.add(week);
        }

        return weeks;
    }
}
