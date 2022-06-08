package com.example.demo.repository;

import com.example.demo.entity.Institute;
import com.example.demo.view.InstitutionsView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstituteRepository extends JpaRepository<Institute, Integer> {
    Institute findById (int instituteId);

    <T> List <T> findBy (Class<T> type);

    <T> List <T> findByModerators_Id (int moderatorId, Class<T> type);
}
