package com.example.demo.repository;

import com.example.demo.entity.Student;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    /*  Топ 10 студентов с фильтрами  */
    <T> List <T> findFirst10ByUser_StatusUser_StatusUserNotInOrderByScoreDesc                       (                       List<String> status, Class<T> type);
    <T> List <T> findFirst10ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc            (int groupId,           List<String> status, Class<T> type);
    <T> List <T> findFirst10ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc           (int streamId,          List<String> status, Class<T> type);
    <T> List <T> findFirst10ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc        (int instituteId,       List<String> status, Class<T> type);
    <T> List <T> findFirst10ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc  (int formOfEducationId, List<String> status, Class<T> type);

    /*  Топ 50 студентов с фильтрами  */
    <T> List <T> findFirst50ByUser_StatusUser_StatusUserNotInOrderByScoreDesc                       (                       List<String> status, Class<T> type);
    <T> List <T> findFirst50ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc            (int groupId,           List<String> status, Class<T> type);
    <T> List <T> findFirst50ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc           (int streamId,          List<String> status, Class<T> type);
    <T> List <T> findFirst50ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc        (int instituteId,       List<String> status, Class<T> type);
    <T> List <T> findFirst50ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc  (int formOfEducationId, List<String> status, Class<T> type);

    /*  Топ 100 студентов с фильтрами  */
    <T> List <T> findFirst100ByUser_StatusUser_StatusUserNotInOrderByScoreDesc                      (                       List<String> status, Class<T> type);
    <T> List <T> findFirst100ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc           (int groupId,           List<String> status, Class<T> type);
    <T> List <T> findFirst100ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc          (int streamId,          List<String> status, Class<T> type);
    <T> List <T> findFirst100ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc       (int instituteId,       List<String> status, Class<T> type);
    <T> List <T> findFirst100ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc (int formOfEducationId, List<String> status, Class<T> type);

    /*  Поиск конкрентного студента  */
    <T> T findById(int studentId, Class<T> type);
    <T> T findByUser(User user, Class<T> type);
    Student findByUser_Id(int userId);

    /*  Поиск по подстроке  */
    <T> List <T> findByUser_LastNameContainingIgnoreCase(String substring, Class<T> type);
    <T> List <T> findByUser_LastNameContainingIgnoreCaseAndUser_StatusUser_StatusUserNotIn(String lastName, List<String> status, Class<T> type);
    <T> List <T> findByUser_LastNameContainingIgnoreCaseAndUser_StatusUser_IdOrderByUser_LastNameAsc(String substring, int status, Class<T> type);

    <T> List <T> findByOrderByUser_LastNameAsc(Class<T> type);
    <T> List <T> findByUser_StatusUser_IdOrderByUser_LastNameAsc(int statusUserId, Class<T> type);

    /*  Поиск по информации об образовании  */
    <T> List <T> findByInstitute_Id(int instituteId, Class<T> type);
    <T> List <T> findByStream_Id(int streamId, Class<T> type);
    <T> List <T> findByGroup_Id(int groupId, Class<T> type);

    /*  Обновленные фильтры для админа  */
    <T> List <T> findByInstitute_IdAndUser_LastNameContainingIgnoreCase(int instituteId, String lastNameSubstring, Class<T> type);
    <T> List <T> findByStream_IdAndUser_LastNameContainingIgnoreCase(int streamId, String lastNameSubstring, Class<T> type);
    <T> List <T> findByGroup_IdAndUser_LastNameContainingIgnoreCase(int groupId, String lastNameSubstring, Class<T> type);

    <T> List <T> findByInstitute_IdAndUser_StatusUser_Id(int instituteId, int statusUser, Class<T> type);
    <T> List <T> findByStream_IdAndUser_StatusUser_Id(int streamId, int statusUser, Class<T> type);
    <T> List <T> findByGroup_IdAndUser_StatusUser_Id(int groupId, int statusUser, Class<T> type);

    <T> List <T> findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(int instituteId, int statusUser, String lastNameSubstring, Class<T> type);
    <T> List <T> findByStream_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(int streamId, int statusUser, String lastNameSubstring, Class<T> type);
    <T> List <T> findByGroup_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(int groupId, int statusUser, String lastNameSubstring, Class<T> type);

    /*  Обновленные фильтры для модера  */
    <T> List <T> findByInstitute_Moderators_IdOrderByUser_LastNameAsc(int moderatorId, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndInstitute_Id(int moderatorId, int instituteId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndStream_Id(int moderatorId, int instituteId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndGroup_Id(int moderatorId, int instituteId, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCase(int moderatorId, String substring, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndInstitute_Id(int moderatorId, String lastNameSubstring, int instituteId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(int moderatorId, String lastNameSubstring, int streamId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(int moderatorId, String lastNameSubstring, int groupId, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdOrderByUser_LastNameAsc(int moderatorId, int statusUserId, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndInstitute_Id(int listInstituteId, int statusUserId, int instituteId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndStream_Id(int listInstituteId, int statusUserId, int streamId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndGroup_Id(int listInstituteId, int statusUserId, int groupId, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndUser_StatusUser_IdOrderByUser_LastNameAsc(int moderatorId, String substring, int status, Class<T> type);

    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndInstitute_Id(int moderatorId, int statusUserId, String lastNameSubstring, int instituteId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(int moderatorId, int statusUserId, String lastNameSubstring, int streamId, Class<T> type);
    <T> List <T> findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(int moderatorId, int statusUserId, String lastNameSubstring, int groupId, Class<T> type);

    /*  Фильтры для сотрудника дирекции  */
    <T> List <T> findByInstitute_IdAndStream_Id(int instituteId, int streamId, Class<T> type);
    <T> List <T> findByInstitute_IdAndGroup_Id(int instituteId, int groupId, Class<T> type);
    <T> List <T> findByInstitute_idAndStream_IdAndUser_LastNameContainingIgnoreCase(int instituteId, int streamId, String lastNameSubstring, Class<T> type);
    <T> List <T> findByInstitute_idAndGroup_IdAndUser_LastNameContainingIgnoreCase(int instituteId, int groupId, String lastNameSubstring, Class<T> type);
    <T> List<T> findByInstitute_idAndStream_IdAndUser_StatusUser_Id(int instituteId, Integer filterId, Integer statusUserId, Class<T> type);
    <T> List<T> findByInstitute_idAndGroup_IdAndUser_StatusUser_Id(int instituteId, Integer filterId, Integer statusUserId, Class<T> type);
    <T> List<T> findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(int instituteId, Integer statusUserId, String substring, Integer filterId, Class<T> type);
    <T> List<T> findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(int instituteId, Integer statusUserId, String substring, Integer filterId, Class<T> type);
    <T> List<T> findFirst10ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst10ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst10ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer integer, List<String> status, Class<T> type);
    <T> List<T> findFirst50ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst50ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst50ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer integer, List<String> status, Class<T> type);
    <T> List<T> findFirst100ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst100ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer filterId, List<String> status, Class<T> type);
    <T> List<T> findFirst100ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(int instituteId, Integer integer, List<String> status, Class<T> type);

}
