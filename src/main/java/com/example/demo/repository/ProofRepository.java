package com.example.demo.repository;

import com.example.demo.entity.ProofAchieve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProofRepository extends JpaRepository<ProofAchieve, Integer> {
    <T> T findById (int proofId, Class<T> type);
    <T> T findByStudent_IdAndId (int studentId, int proofId, Class<T> type);
    <T> T findFirstByOrderByIdDesc (Class<T> type);
    <T> T findByAchievement_IdAndStudent_IdAndStatusRequest_IdNot (int achieveId, int studentId, int statusId, Class<T> type);
    <T> List <T> findByStudent_IdAndStatusRequest_IdNot (int studentId, int statusId, Class<T> type);

    <T> List <T> findByStudent_Id (int studentId, Class<T> type);
    <T> List <T> findByStudent_Institute_Moderators_IdOrderByDateProofDesc(int moderatorId, Class<T> type);
    <T> List <T> findByStatusRequest_IdAndStudent_Institute_Moderators_IdOrderByDateProofDesc (int statusId, int moderatorId, Class<T> type);
}
