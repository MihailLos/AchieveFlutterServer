package com.example.demo.controller;

import com.example.demo.controller.request.PhotoRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.*;
import com.example.demo.view.*;
import com.example.demo.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(description = "Контроллер студента")
@RestController
public class StudentController {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private StudentService studentService;
    @Autowired
    private FileService fileService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private UserService userService;
    @Autowired
    private LogService logService;
    @Autowired
    private ModeratorService moderatorService;


    //////////////////////////////////////////////////////////////////////////////////
    /*                               Студент                                        */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Просмотр данных профиля - для студента")
    @GetMapping("/student/getStudent")
    public StudentView getStudentById(@RequestParam
                                          @ApiParam (value = "Id студента, чей профиль мы просматриваем. Не передается, если студент смотрит свой профиль. Если чужой профиль - Not null. >0", example = "12")
                                                  Optional<Integer> studentId) {
        if (studentId.isEmpty())
            studentId = Optional.of(studentService.getStudentId());
        StudentView student = studentService.getStudentById(studentId.get(), StudentView.class);
        if(student!=null)
            return student;
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");
    }

    @ApiOperation("Просмотр данных об образовании - для студента")
    @GetMapping("/student/getStudy")
    public StudyView getStudentStudy(@RequestParam
                                         @ApiParam (value = "Id студента, чей профиль мы просматриваем. Не передается, если студент смотрит свой профиль. Если чужой профиль - Not null. >0", example = "12")
                                                 Optional<Integer> studentId) {
        if (studentId.isEmpty())
            studentId = Optional.of(studentService.getStudentId());
        StudyView student = studentService.getStudentById(studentId.get(), StudyView.class);
        if(student!=null)
            return student;
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");
    }

    @ApiOperation("Топ 10 студентов с фильтами - для студента")
    @GetMapping("/student/students10")
    public List<StudentsView> getRating10(@RequestParam
                                              @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                      Optional<String> substring,
                                          @RequestParam
                                              @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Институт", allowableValues = "Форма,Институт,Направление,Группа")
                                                      Optional<String> filterName,
                                          @RequestParam
                                              @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                      Optional<Integer> filterId) {
        if(substring.isEmpty())
            return studentService.getTop10Students(filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentBySubstring(substring.get(), StudentsView.class);
    }

    @ApiOperation("Топ 50 студентов с фильтами - для студента")
    @GetMapping("/student/students50")
    public List<StudentsView> getRating50(@RequestParam
                                              @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                      Optional<String> substring,
                                          @RequestParam
                                              @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Направление", allowableValues = "Форма,Институт,Направление,Группа")
                                                      Optional<String> filterName,
                                          @RequestParam
                                              @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                      Optional<Integer> filterId) {
        if(substring.isEmpty())
            return studentService.getTop50Students(filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentBySubstring(substring.get(), StudentsView.class);
    }

    @ApiOperation("Топ 100 студентов с фильтами - для студента")
    @GetMapping("/student/students100")
    public List<StudentsView> getRating100(@RequestParam
                                               @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                       Optional<String> substring,
                                           @RequestParam
                                               @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Направление", allowableValues = "Форма,Институт,Направление,Группа")
                                                       Optional<String> filterName,
                                           @RequestParam
                                               @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                       Optional<Integer> filterId) {
        if(substring.isEmpty())
            return studentService.getTop100Students(filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentBySubstring(substring.get(), StudentsView.class);
    }

    @ApiOperation("Удаление аккаунта студентом")
    @PutMapping("/student/deleteAccount")
    public SuccessfullResponse deleteAccount() {
        User user = userService.getUser();
        user.setStatusUser(userService.getStatusUser("Удален"));
        userService.saveUser(user);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Аккаунт успешно удален. Вы всегда можете восстановить аккаунт через указанную при регистрации почту");
    }

    @ApiOperation("Изменение фотографии профиля")
    @PutMapping("/student/changePhoto")
    public ResponseEntity changePhoto(@RequestBody
                                          @ApiParam(value = "Запрос с данными для изменения фотографии профиля")
                                                  PhotoRequest request) {
        //Получаем авторизованного студента
        int studentId = studentService.getStudentId();
        Student student = studentService.getStudentById(studentId, Student.class);

        //Проверяем, что полученный файл не пуст
        if (request.getData().length==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Фото пустое");

        //Проверяем стандартное фото(23 id) стоит или нет.
        //Если было фото по умолчанию, то создаем новый файл
        if (student.getFileStudent().getIdFile()==23) {
            File file = new File();
            file.setFormatFile(request.getFormat());
            file.setDataFile(request.getData());
            file.setListFile(null);
            fileService.saveFile(file);
            student.setFileStudent(file);
            studentService.saveStudent(student);
            return ResponseEntity.status(HttpStatus.OK).body("Фотограция профиля успешно изменена");
        }
        //Если фото совпадает с предыдущим, выводим соответствующее сообщение
        if (Arrays.equals(student.getFileStudent().getDataFile(), request.getData()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Новое фото не может совпадать с предыдущим");

        //Если нестрандартное изображение, то меняем уже существующий файл
        return fileService.changePhoto(student, request.getData(), request.getFormat());
    }

    //////////////////////////////////////////////////////////////////////////////////
    /*                                   Админ                                      */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список студентов - для админа")
    @GetMapping("/admin/students")
    public List<StudentsAdminView> getStudentsForAdmin(@RequestParam
                                                      @ApiParam(value = "Подстрока с фамилией (или ее частью) студента. Фильтр, может не передаваться. Если передается - Not null", example = "Астахо")
                                                              Optional<String> substring,
                                                  @RequestParam
                                                    @ApiParam(value = "Id статуса пользователя. Not null. [1,4]", example = "1")
                                                          Optional<Integer> statusUserId,
                                                       @RequestParam
                                                           @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Направление", allowableValues = "Институт,Направление,Группа")
                                                                   Optional<String> filterName,
                                                       @RequestParam
                                                           @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                                   Optional<Integer> filterId) {
        if (statusUserId.isEmpty()) {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getAllStudentsForAdmin(StudentsAdminView.class);
                else
                    return studentService.getStudentsByEducation(filterName.get(), filterId.get(), StudentsAdminView.class);
            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameForAdmin(substring.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentByLastNameAndEducationForAdmin(substring.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }
        else {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getStudentsByStatusUserIdForAdmin(statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentByStatusUserIdAndEducationForAdmin(statusUserId.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameAndStatusUserIdForAdmin(substring.get(), statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentByLastNameAndStatusUserIdAndEducationForAdmin(substring.get(), statusUserId.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }
    }

    @ApiOperation("Список студентов - для модера")
    @GetMapping("/moderator/students")
    public List<StudentsAdminView> getStudentsForModer(@RequestParam
                                                       @ApiParam(value = "Подстрока с фамилией (или ее частью) студента. Фильтр, может не передаваться. Если передается - Not null", example = "Астахо")
                                                               Optional<String> substring,
                                                       @RequestParam
                                                       @ApiParam(value = "Id статуса пользователя. Not null. [1,4]", example = "1")
                                                               Optional<Integer> statusUserId,
                                                       @RequestParam
                                                       @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Направление", allowableValues = "Форма,Институт,Направление,Группа")
                                                               Optional<String> filterName,
                                                       @RequestParam
                                                       @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                               Optional<Integer> filterId) {

        User currentUser = userService.getUser();
        Moderator currentModerator = userService.getModeratorByUserId(currentUser.getIdUser(), Moderator.class);

        if (statusUserId.isEmpty()) {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getAllStudentsForModerNew(currentModerator, StudentsAdminView.class);
                else
                    return studentService.getStudentsByEducationForModerNew(currentModerator, filterName.get(), filterId.get(), StudentsAdminView.class);

            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameForModerNew(currentModerator, substring.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByLastNameAndEducationForModerNew(currentModerator, substring.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }

        else {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getStudentsByStatusUserIdForModerNew(currentModerator, statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByStatusUserIdAndEducationForModerNew(currentModerator, statusUserId.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameAndStatusUserIdForModerNew(currentModerator, substring.get(), statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByStatusUserIdAndLastNameAndEducationForModerNew(currentModerator, statusUserId.get(), substring.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }
    }

    @ApiOperation("Список студентов - для сотрудника дирекции")
    @GetMapping("/dekanat/students")
    public List<StudentsAdminView> getStudentsForDekanat(@RequestParam
                                                       @ApiParam(value = "Подстрока с фамилией (или ее частью) студента. Фильтр, может не передаваться. Если передается - Not null", example = "Астахо")
                                                               Optional<String> substring,
                                                       @RequestParam
                                                       @ApiParam(value = "Id статуса пользователя. Not null. [1,4]", example = "1")
                                                               Optional<Integer> statusUserId,
                                                       @RequestParam
                                                       @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Направление", allowableValues = "Форма,Направление,Группа")
                                                               Optional<String> filterName,
                                                       @RequestParam
                                                       @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                               Optional<Integer> filterId) {
        User currentUser = userService.getUser();
        Dekanat currentDekanat = userService.getDekanatByUserId(currentUser.getIdUser(), Dekanat.class);
        int dekanatInstituteId = currentDekanat.getInstitute().getIdInstitute();

        if (statusUserId.isEmpty()) {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getAllStudentsForDekanat(dekanatInstituteId, StudentsAdminView.class);
                else
                    return studentService.getStudentsByEducationForDekanat(dekanatInstituteId, filterName.get(), filterId.get(), StudentsAdminView.class);

            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameForDekanat(dekanatInstituteId, substring.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByLastNameAndEducationForDekanat(dekanatInstituteId, substring.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }

        else {
            if(substring.isEmpty()) {
                if (filterName.isEmpty())
                    return studentService.getStudentsByStatusUserIdForDekanat(dekanatInstituteId, statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByStatusUserIdAndEducationForDekanat(dekanatInstituteId, statusUserId.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
            else {
                if (filterName.isEmpty())
                    return studentService.getStudentsByLastNameAndStatusUserIdForDekanat(dekanatInstituteId, substring.get(), statusUserId.get(), StudentsAdminView.class);
                else
                    return studentService.getStudentsByStatusUserIdAndLastNameAndEducationForDekanat(dekanatInstituteId, statusUserId.get(), substring.get(), filterName.get(), filterId.get(), StudentsAdminView.class);
            }
        }
    }

    @ApiOperation("Конкретный студент - для админа/модера/сотрудника дирекции")
    @GetMapping("/upr/students/{studentId}")
    public StudentAdminView getByIdForAdmin(@PathVariable
                                                @ApiParam(value = "Id студента", example = "10")
                                                        Integer studentId) {
        StudentAdminView student = studentService.getStudentById(studentId, StudentAdminView.class);
        if(student!=null)
            return student;
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");
    }

    @ApiOperation("Удаление студента без права восстановления - для модератора")
    @PutMapping("/moderator/deleteStudent/{studentId}")
    public SuccessfullResponse deleteStudentByIdForModer(@PathVariable
                                                        @ApiParam(value = "Id студента. Not null. >0", example = "18")
                                                                int studentId) throws MessagingException {
        Student student = studentService.getStudentById(studentId, Student.class);
        User findUser = student.getUserForStudent();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Ваш аккаунт удален!");
        helper.setText("Ваш аккаунт в информационной системе учета достиженй студентов удален без права восстановления!");

        if (student==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");

        User user = userService.getUserById(student.getUserForStudent().getIdUser(), User.class);
        //Если пользователь найден, то меняем его статус
        if(user!=null) {
            String oldData = user.getStatusUser().getStatusUser();
            user.setStatusUser(userService.getStatusUser("Аннигилирован"));
            userService.saveUser(user);
            logService.createNewLog(userService.getUserId(), 15, user.getIdUser(), oldData, user.getStatusUser().getStatusUser());
            mailSender.send(notificationMessage);
            return new SuccessfullResponse(HttpStatus.OK.value(), "Студент удален без права на восстановление");
        }
        //Иначе выводим соответствующее сообщение
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
    }

    @ApiOperation("Топ 10 студентов с фильтами - для сотрудника дирекции")
    @GetMapping("/dekanat/students10")
    public List<StudentsView> getRating10ForDekanat(@RequestParam
                                          @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                  Optional<String> substring,
                                          @RequestParam
                                          @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Форма", allowableValues = "Форма,Направление,Группа")
                                                  Optional<String> filterName,
                                          @RequestParam
                                          @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                  Optional<Integer> filterId) {
        User currentUser = userService.getUser();
        Dekanat currentDekanat = userService.getDekanatByUserId(currentUser.getIdUser(), Dekanat.class);
        int dekanatInstituteId = currentDekanat.getInstitute().getIdInstitute();

        if(substring.isEmpty())
            return studentService.getTop10StudentsForDekanat(dekanatInstituteId, filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentsByLastNameForDekanat(dekanatInstituteId, substring.get(), StudentsView.class);
    }

    @ApiOperation("Топ 50 студентов с фильтами - для сотрудника дирекции")
    @GetMapping("/dekanat/students50")
    public List<StudentsView> getRating50ForDekanat(@RequestParam
                                                    @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                            Optional<String> substring,
                                                    @RequestParam
                                                    @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Форма", allowableValues = "Форма,Направление,Группа")
                                                            Optional<String> filterName,
                                                    @RequestParam
                                                    @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                            Optional<Integer> filterId) {
        User currentUser = userService.getUser();
        Dekanat currentDekanat = userService.getDekanatByUserId(currentUser.getIdUser(), Dekanat.class);
        int dekanatInstituteId = currentDekanat.getInstitute().getIdInstitute();

        if(substring.isEmpty())
            return studentService.getTop50StudentsForDekanat(dekanatInstituteId, filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentsByLastNameForDekanat(dekanatInstituteId, substring.get(), StudentsView.class);
    }

    @ApiOperation("Топ 100 студентов с фильтами - для сотрудника дирекции")
    @GetMapping("/dekanat/students100")
    public List<StudentsView> getRating100ForDekanat(@RequestParam
                                                    @ApiParam(value = "Фамилия(или ее часть) студента, чей профиль мы ищем", example = "аст")
                                                            Optional<String> substring,
                                                    @RequestParam
                                                    @ApiParam(value = "Название фильтра. Передается в паре с id", example = "Форма", allowableValues = "Форма,Направление,Группа")
                                                            Optional<String> filterName,
                                                    @RequestParam
                                                    @ApiParam(value = "id группы, направления или др. (зависит от фильтра). Передается в паре с фильтром", example = "3")
                                                            Optional<Integer> filterId) {
        User currentUser = userService.getUser();
        Dekanat currentDekanat = userService.getDekanatByUserId(currentUser.getIdUser(), Dekanat.class);
        int dekanatInstituteId = currentDekanat.getInstitute().getIdInstitute();

        if(substring.isEmpty())
            return studentService.getTop100StudentsForDekanat(dekanatInstituteId, filterName, filterId, StudentsView.class);
        else
            return studentService.getStudentsByLastNameForDekanat(dekanatInstituteId, substring.get(), StudentsView.class);
    }
}
