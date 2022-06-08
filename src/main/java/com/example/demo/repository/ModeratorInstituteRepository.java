package com.example.demo.repository;

import com.example.demo.entity.ListInstitute;
import com.example.demo.entity.ModeratorInstitute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModeratorInstituteRepository extends JpaRepository<ModeratorInstitute, Integer> {
    ModeratorInstitute findFirstByOrderByIdDesc();
    ModeratorInstitute findByModeratorIdAndInstituteId(int moderatorId, int instituteId);
    List<ModeratorInstitute> findByModeratorId(int moderatorId);
}
