package com.example.demo.repository;

import com.example.demo.entity.Dekanat;
import com.example.demo.entity.Moderator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DekanatRepository extends JpaRepository<Dekanat, Integer> {
    <T> List <T> findBy (Class<T> type);

    <T> T findById (int dekanatId, Class<T> type);
    <T> T findByUser_Id (int userId, Class<T> type);
    Dekanat findByUser_Id (int userId);
}
