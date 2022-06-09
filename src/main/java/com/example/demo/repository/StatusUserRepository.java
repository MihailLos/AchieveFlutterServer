package com.example.demo.repository;

import com.example.demo.entity.StatusUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusUserRepository extends JpaRepository<StatusUser, Integer> {
    StatusUser findByStatusUser (String name);

    <T> List<T> findBy (Class<T> type);
}
