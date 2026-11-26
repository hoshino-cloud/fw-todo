package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> search(String keyword, String category, String order) {
        return search(keyword, category, order, null, null);
    }

    public List<Todo> search(String keyword, String category, String order, boolean includeCompleted) {
        return todoMapper.search(keyword, category, order, null, null, includeCompleted, false);
    }

    public List<Todo> searchPage(String keyword, String category, String order, boolean includeCompleted,
            boolean trash, int pageSize, int page) {
        return todoMapper.searchPage(keyword, category, order, includeCompleted, trash, pageSize,
                (page - 1) * pageSize);
    }

    public int count(String keyword, String category, boolean includeCompleted, boolean trash) {
        return todoMapper.count(keyword, category, includeCompleted, trash);
    }

    public List<Todo> search(String keyword, String category, String order, LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, from, to, null, false);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public int create(Todo todo) {
        int result = todoMapper.insert(todo);
        log.info("Todo created: id={}", todo.getId());
        return result;
    }

    public int update(Todo todo) {
        int result = todoMapper.update(todo);
        log.info("Todo updated: id={}", todo.getId());
        return result;
    }

    public int delete(Long id) {
        int result = todoMapper.markDeletedById(id);
        log.info("Todo deleted: id={}", id);
        return result;
    }

    public int restore(Long id) {
        int result = todoMapper.restoreById(id);
        log.info("Todo restored: id={}", id);
        return result;
    }
}
