package com.example.demo.repository;

import com.example.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findById(int categoryId);
    <T> T findById(int categoryId, Class<T> type);
    Category findByName(String categoryName);

    <T> List <T> findBy(Class<T> type);
}
