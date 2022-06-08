package com.example.demo.service;

import com.example.demo.config.CustomUserDetails;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.view.StudentsAdminView;
import com.example.demo.view.StudentsView;
import org.dom4j.rule.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StudentService {

    @Autowired
    private StreamRepository streamRepository;
    @Autowired
    private InstituteRepository instituteRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private FormEducationRepository formEducationRepository;
    @Autowired
    private StudentRepository studentRepository;

    //Получаем айди текущего авторизованного студента из токена
    public int getStudentId() {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int userId = customUserDetails.getId();
        return studentRepository.findByUser_Id(userId).getIdStudent();
    }

    public Student getStudent(User user) {
        return studentRepository.findByUser(user, Student.class);
    }

    //Статусы, при которых студент не отображается в рейтингах/списках для студента
    private List<String> getStatus() {
        List<String> status = new ArrayList<>();
        status.add("Удален");
        status.add("Аннигилирован");
        status.add("Забанен");
        return status;
    }

    //Создаем нового студента
    public ResponseEntity createNewStudent(int groupId, int formEducationId, int streamId, int instituteId, Student student) {
        student.setScoreStudent(0);

        FormOfEducation formOfEducation = formEducationRepository.findById(formEducationId);
        if (formOfEducation==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Форма обучения с указанным id не найдена");
        student.setFormOfEducationStudent(formOfEducation);

        Institute institute = instituteRepository.findById(instituteId);
        if (institute==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Институт с указанным id не найден");
        student.setInstituteStudent(institute);

        Stream stream = streamRepository.findById(streamId);
        if (stream==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Направление с указанным id не найдено или принадлежит другому институту");
        student.setStreamStudent(stream);

        Group group = groupRepository.findById(groupId);
        if (group==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Группа с указанным id не найдена или принадлежит другому направлению");
        student.setGroupStudent(group);

        studentRepository.save(student);
        return ResponseEntity.status(HttpStatus.OK).body("Регистрация прошла успешно");
    }

    public Student createNewEiosStudent(Student student) {
        studentRepository.save(student);
        return student;
    }

    //Топ 10 рейтинга студентов
    public <T> List <T> getTop10Students(Optional<String> filterName, Optional<Integer> idForFilter, Class<T> type) {
        if(filterName.isEmpty())
            return studentRepository.findFirst10ByUser_StatusUser_StatusUserNotInOrderByScoreDesc(getStatus(), type);
        if (filterName.get().equals("Группа"))
            return studentRepository.findFirst10ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(idForFilter.get(), getStatus(), type);
        if (filterName.get().equals("Направление"))
            return studentRepository.findFirst10ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(idForFilter.get(), getStatus(), type);
        if (filterName.get().equals("Форма"))
            return studentRepository.findFirst10ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(idForFilter.get(), getStatus(), type);
        if (filterName.get().equals("Институт"))
            return studentRepository.findFirst10ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(idForFilter.get(), getStatus(), type);
        return null;
    }

    //Топ 50 рейтинга студентов
    public <T> List <T> getTop50Students(Optional<String> filter, Optional<Integer> id, Class<T> type) {
        if(filter.isEmpty())
            return studentRepository.findFirst50ByUser_StatusUser_StatusUserNotInOrderByScoreDesc(getStatus(), type);

        if (filter.get().equals("Группа"))
            return studentRepository.findFirst50ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Направление"))
            return studentRepository.findFirst50ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Форма"))
            return studentRepository.findFirst50ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Институт"))
            return studentRepository.findFirst50ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        return null;
    }

    //Топ 100 рейтинга студентов
    public <T> List <T> getTop100Students(Optional<String> filter, Optional<Integer> id, Class<T> type) {
        if(filter.isEmpty())
            return studentRepository.findFirst100ByUser_StatusUser_StatusUserNotInOrderByScoreDesc(getStatus(), type);
        if (filter.get().equals("Группа"))
            return studentRepository.findFirst100ByGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Направление"))
            return studentRepository.findFirst100ByStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Форма"))
            return studentRepository.findFirst100ByFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        if (filter.get().equals("Институт"))
            return studentRepository.findFirst100ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(id.get(), getStatus(), type);
        return null;
    }

    // Список студентов по информации об образовании
    public <T> List <T> getStudentsByEducation(String filter, Integer id, Class<T> type) {
        if (filter.equals("Группа"))
            return studentRepository.findByGroup_Id(id, type);
        if (filter.equals("Направление"))
            return studentRepository.findByStream_Id(id, type);
        if (filter.equals("Институт"))
            return studentRepository.findByInstitute_Id(id, type);
        return null;
    }

    //Получаем студента по его id
    public <T> T getStudentById(int studentId, Class<T> type) {
        return studentRepository.findById(studentId, type);
    }


    //Сохраняем студента
    public void saveStudent(Student student) {
        studentRepository.save(student);
    }

    //Ищем студентов по фамилии или ее части для студента
    public <T> List <T> getStudentBySubstring(String substring, Class<T> type) {
        return studentRepository.findByUser_LastNameContainingIgnoreCaseAndUser_StatusUser_StatusUserNotIn(substring, getStatus(), type);
    }

    //Получаем список всех студентов для админа
    public <T> List <T> getAllStudentsForAdmin(Class<T> type) {
        return studentRepository.findByOrderByUser_LastNameAsc(type);
    }

    public <T> List <T> getAllStudentsForModerNew(Moderator moderator, Class<T> type) {
        return studentRepository.findByInstitute_Moderators_IdOrderByUser_LastNameAsc(moderator.getIdModerator(), type);
    }

    //Ищем студентов по фамилии или ее части для админа
    public <T> List <T> getStudentsByLastNameForAdmin(String substring, Class<T> type) {
        return studentRepository.findByUser_LastNameContainingIgnoreCase(substring, type);
    }

    public <T> List <T> getStudentsByLastNameForModerNew(Moderator moderator, String substring, Class<T> type) {
        return studentRepository.findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCase(moderator.getIdModerator(), substring, type);
    }

    //Получаем список студентов по id их статуса для админа
    public <T> List <T> getStudentsByStatusUserIdForAdmin(int statusUserId, Class<T> type) {
        return studentRepository.findByUser_StatusUser_IdOrderByUser_LastNameAsc(statusUserId, type);
    }

    public <T> List <T> getStudentsByStatusUserIdForModerNew(Moderator moderator, int statusUserId, Class<T> type) {
        return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdOrderByUser_LastNameAsc(moderator.getIdModerator(), statusUserId, type);
    }

    //Ищем студентов по фамилии или ее части и по id их статуса для админа
    public <T> List <T> getStudentsByLastNameAndStatusUserIdForAdmin(String substring, int status, Class<T> type) {
        return studentRepository.findByUser_LastNameContainingIgnoreCaseAndUser_StatusUser_IdOrderByUser_LastNameAsc(substring, status, type);
    }

    public <T> List <T> getStudentsByLastNameAndStatusUserIdForModerNew(Moderator moderator, String substring, int status, Class<T> type) {
        return studentRepository.findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndUser_StatusUser_IdOrderByUser_LastNameAsc(moderator.getIdModerator(), substring, status, type);
    }

    public <T> List <T> getStudentByLastNameAndEducationForAdmin(String substring, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_IdAndUser_LastNameContainingIgnoreCase(filterId, substring, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByStream_IdAndUser_LastNameContainingIgnoreCase(filterId, substring, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByGroup_IdAndUser_LastNameContainingIgnoreCase(filterId, substring, type);
        return null;
    }

    public <T> List<T> getStudentByStatusUserIdAndEducationForAdmin(Integer statusUserId, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_IdAndUser_StatusUser_Id(filterId, statusUserId, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByStream_IdAndUser_StatusUser_Id(filterId, statusUserId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByGroup_IdAndUser_StatusUser_Id(filterId, statusUserId, type);
        return null;
    }

    public <T> List<T> getStudentByLastNameAndStatusUserIdAndEducationForAdmin(String substring, Integer statusUserId, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(filterId, statusUserId, substring, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByStream_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(filterId, statusUserId, substring, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByGroup_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(filterId, statusUserId, substring, type);
        return null;
    }

    public <T> List<T> getStudentsByEducationForModerNew(Moderator moderator, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_Moderators_IdAndInstitute_Id(moderator.getIdModerator(), filterId, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_Moderators_IdAndStream_Id(moderator.getIdModerator(), filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_Moderators_IdAndGroup_Id(moderator.getIdModerator(), filterId, type);
        return null;
    }

    public <T> List<T> getStudentsByLastNameAndEducationForModerNew(Moderator moderator, String lastNameSubstring, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndInstitute_Id(moderator.getIdModerator(), lastNameSubstring, filterId, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(moderator.getIdModerator(), lastNameSubstring, filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(moderator.getIdModerator(), lastNameSubstring, filterId, type);
        return null;
    }

    public <T> List<T> getStudentsByStatusUserIdAndEducationForModerNew(Moderator moderator, Integer statusId, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndInstitute_Id(moderator.getIdModerator(), statusId, filterId, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndStream_Id(moderator.getIdModerator(), statusId, filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndGroup_Id(moderator.getIdModerator(), statusId, filterId, type);
        return null;
    }

    public <T> List<T> getStudentsByStatusUserIdAndLastNameAndEducationForModerNew(Moderator moderator, Integer statusUserId, String lastNameSubstring, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Институт"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndInstitute_Id(moderator.getIdModerator(), statusUserId, lastNameSubstring, filterId, type);
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(moderator.getIdModerator(), statusUserId, lastNameSubstring, filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_Moderators_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(moderator.getIdModerator(), statusUserId, lastNameSubstring, filterId, type);
        return null;
    }

    public <T> List<T> getAllStudentsForDekanat(int instituteId, Class<T> type) {
        return studentRepository.findByInstitute_Id(instituteId, type);
    }

    public <T> List<T> getStudentsByEducationForDekanat(int instituteId, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_IdAndStream_Id(instituteId, filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_IdAndGroup_Id(instituteId, filterId, type);
        return null;
    }

    public <T> List<T> getStudentsByLastNameForDekanat(int instituteId, String substring, Class<T> type) {
        return studentRepository.findByInstitute_IdAndUser_LastNameContainingIgnoreCase(instituteId, substring, type);
    }

    public <T> List<T> getStudentsByLastNameAndEducationForDekanat(int instituteId, String substring, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_idAndStream_IdAndUser_LastNameContainingIgnoreCase(instituteId, filterId, substring, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_idAndGroup_IdAndUser_LastNameContainingIgnoreCase(instituteId, filterId, substring, type);
        return null;
    }

    public <T> List<T> getStudentsByStatusUserIdForDekanat(int instituteId, Integer statusUserId, Class<T> type) {
        return studentRepository.findByInstitute_IdAndUser_StatusUser_Id(instituteId, statusUserId, type);
    }

    public <T> List<T> getStudentsByStatusUserIdAndEducationForDekanat(int instituteId, Integer statusUserId, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_idAndStream_IdAndUser_StatusUser_Id(instituteId, filterId, statusUserId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_idAndGroup_IdAndUser_StatusUser_Id(instituteId, filterId, statusUserId, type);
        return null;
    }

    public <T> List<T> getStudentsByLastNameAndStatusUserIdForDekanat(int instituteId, String substring, Integer statusUserId, Class<T> type) {
        return studentRepository.findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCase(instituteId, statusUserId, substring, type);
    }

    public <T> List<T> getStudentsByStatusUserIdAndLastNameAndEducationForDekanat(int instituteId, Integer statusUserId, String substring, String filterName, Integer filterId, Class<T> type) {
        if (filterName.equals("Направление"))
            return studentRepository.findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndStream_Id(instituteId, statusUserId, substring, filterId, type);
        if (filterName.equals("Группа"))
            return studentRepository.findByInstitute_IdAndUser_StatusUser_IdAndUser_LastNameContainingIgnoreCaseAndGroup_Id(instituteId, statusUserId, substring, filterId, type);
        return null;
    }

    public <T> List<T> getTop10StudentsForDekanat(int instituteId, Optional<String> filterName, Optional<Integer> filterId, Class<T> type) {
        if(filterName.isEmpty())
            return studentRepository.findFirst10ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, getStatus(), type);
        if (filterName.get().equals("Группа"))
            return studentRepository.findFirst10ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Направление"))
            return studentRepository.findFirst10ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Форма"))
            return studentRepository.findFirst10ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        return null;
    }

    public <T> List<T> getTop50StudentsForDekanat(int instituteId, Optional<String> filterName, Optional<Integer> filterId, Class<T> type) {
        if(filterName.isEmpty())
            return studentRepository.findFirst50ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, getStatus(), type);
        if (filterName.get().equals("Группа"))
            return studentRepository.findFirst50ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Направление"))
            return studentRepository.findFirst50ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Форма"))
            return studentRepository.findFirst50ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        return null;
    }

    public <T> List<T> getTop100StudentsForDekanat(int instituteId, Optional<String> filterName, Optional<Integer> filterId, Class<T> type) {
        if(filterName.isEmpty())
            return studentRepository.findFirst100ByInstitute_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, getStatus(), type);
        if (filterName.get().equals("Группа"))
            return studentRepository.findFirst100ByInstitute_IdAndGroup_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Направление"))
            return studentRepository.findFirst100ByInstitute_IdAndStream_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        if (filterName.get().equals("Форма"))
            return studentRepository.findFirst100ByInstitute_IdAndFormOfEducation_IdAndUser_StatusUser_StatusUserNotInOrderByScoreDesc(instituteId, filterId.get(), getStatus(), type);
        return null;
    }
}
