package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    List<Todo> search(@Param("keyword") String keyword, @Param("category") String category,
            @Param("order") String order, @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("includeCompleted") Boolean includeCompleted);

    List<Todo> searchPage(@Param("keyword") String keyword, @Param("category") String category,
            @Param("order") String order, @Param("includeCompleted") boolean includeCompleted,
            @Param("limit") int limit, @Param("offset") int offset);

    int count(@Param("keyword") String keyword, @Param("category") String category,
            @Param("includeCompleted") boolean includeCompleted);

    Todo findById(@Param("id") Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(@Param("id") Long id);
}
