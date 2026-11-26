package com.example.todoapp.mcp;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import com.example.todoapp.api.HolidayClient;
import com.example.todoapp.api.TodoDto;

@Component
public class TodoTools {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "デザイン", "マーケティング", "プログラミング", "資格", "就職活動");

    private final TodoService todoService;
    private final HolidayClient holidayClient;

    public TodoTools(TodoService todoService, HolidayClient holidayClient) {
        this.todoService = todoService;
        this.holidayClient = holidayClient;
    }

    @McpTool(name = "list_todos", description = "やることの一覧を返す（期間・ジャンルで絞れる）")
    public List<TodoDto> listTodos(
            @McpToolParam(required = false) String keyword,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) String from,
            @McpToolParam(required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        return todoService.search(keyword, category, "asc", fromDate, toDate).stream()
                .map(TodoDto::from)
                .toList();
    }

    @McpTool(name = "summarize_week", description = "期間内のやることを一覧ではなく、件数・ジャンルごとの件数・未完了で期限が過ぎた件数に集計して返す")
    public Map<String, Object> summarizeWeek(
            @McpToolParam(required = false) String from,
            @McpToolParam(required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください");
        }

        List<Todo> todos = todoService.search(null, null, "asc", fromDate, toDate);
        Map<String, Long> categoryCounts = todos.stream()
                .collect(Collectors.groupingBy(Todo::getCategory, LinkedHashMap::new, Collectors.counting()));
        long overdueCount = todos.stream()
                .filter(todo -> !Boolean.TRUE.equals(todo.getCompleted()))
                .map(Todo::getDueDate)
                .filter(java.util.Objects::nonNull)
                .filter(dueDate -> dueDate.isBefore(LocalDate.now()))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", todos.size());
        summary.put("categoryCounts", categoryCounts);
        summary.put("overdueCount", overdueCount);
        return summary;
    }

    @McpTool(name = "get_todo", description = "やることを1件返す")
    public TodoDto getTodo(@McpToolParam(required = true) Long id) {
        return TodoDto.from(requireTodo(id));
    }

    @McpTool(name = "add_todo", description = "やることを1件足す")
    public TodoDto addTodo(
            @McpToolParam(required = true) String title,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = true) String category,
            @McpToolParam(required = true) Integer priority,
            @McpToolParam(required = false) String dueDate) {
        validateCategory(category);
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDetail(detail);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        todo.setCompleted(false);
        todoService.create(todo);
        return TodoDto.from(todoService.findById(todo.getId()));
    }

    @McpTool(name = "update_todo", description = "やることを1件直す（期限を変えるのもこれ）")
    public TodoDto updateTodo(
            @McpToolParam(required = true) Long id,
            @McpToolParam(required = false) String title,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) Integer priority,
            @McpToolParam(required = false) String dueDate,
            @McpToolParam(required = false) Boolean completed) {
        Todo todo = requireTodo(id);
        if (title != null) {
            todo.setTitle(title);
        }
        if (detail != null) {
            todo.setDetail(detail);
        }
        if (category != null) {
            validateCategory(category);
            todo.setCategory(category);
        }
        if (priority != null) {
            todo.setPriority(priority);
        }
        if (dueDate != null) {
            todo.setDueDate(LocalDate.parse(dueDate));
        }
        if (completed != null) {
            todo.setCompleted(completed);
        }
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "complete_todo", description = "やることを完了にする")
    public TodoDto completeTodo(@McpToolParam(required = true) Long id) {
        Todo todo = requireTodo(id);
        todo.setCompleted(true);
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "delete_todo", description = "やることを1件消す")
    public void deleteTodo(@McpToolParam(required = true) Long id) {
        todoService.delete(id);
    }

    @McpTool(name = "find_free_days", description = "期間の中で、期限のやることが無く、土日でも祝日でもない「空いている日」を返す。やることの期限を動かす先を決めるのに使う")
    public List<LocalDate> findFreeDays(
            @McpToolParam(required = true) String from,
            @McpToolParam(required = true) String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください");
        }

        Set<LocalDate> todoDueDates = todoService.search(null, null, "asc", fromDate, toDate).stream()
                .map(Todo::getDueDate)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> holidayDates = holidayClient.getHolidays().holidays().keySet();

        return fromDate.datesUntil(toDate.plusDays(1))
                .filter(date -> !todoDueDates.contains(date))
                .filter(date -> date.getDayOfWeek().getValue() < 6)
                .filter(date -> !holidayDates.contains(date.toString()))
                .toList();
    }

    private Todo requireTodo(Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            throw new IllegalArgumentException("Todo が見つかりません: " + id);
        }
        return todo;
    }

    private void validateCategory(String category) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("ジャンルは デザイン・マーケティング・プログラミング・資格・就職活動 のいずれかにしてください");
        }
    }
}
