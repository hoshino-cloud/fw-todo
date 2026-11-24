package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
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
            Model model) {
        LocalDate today = LocalDate.now();
        YearMonth displayedMonth = YearMonth.of(
                year != null ? year : today.getYear(),
                month != null ? month : today.getMonthValue());

        LocalDate firstDay = displayedMonth.atDay(1);
        LocalDate lastDay = displayedMonth.atEndOfMonth();

        model.addAttribute("displayedMonth", displayedMonth);
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("weeks", createWeeks(firstDay, lastDay));
        model.addAttribute("previousMonth", displayedMonth.minusMonths(1));
        model.addAttribute("nextMonth", displayedMonth.plusMonths(1));
        return "calendar";
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
