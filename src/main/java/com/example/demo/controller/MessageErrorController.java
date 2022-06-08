package com.example.demo.controller;

import com.example.demo.controller.request.ChangeCommentRequest;
import com.example.demo.controller.request.CreationErrorRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.MessageError;
import com.example.demo.entity.StatusMessageError;
import com.example.demo.entity.Student;
import com.example.demo.entity.User;
import com.example.demo.view.*;
import com.example.demo.service.LogService;
import com.example.demo.service.MessageErrorService;
import com.example.demo.service.StudentService;
import com.example.demo.service.UserService;
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
import java.util.List;
import java.util.Optional;

@Api(description = "Контроллер сообщений об ошибке")
@RestController
public class MessageErrorController {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private StudentService studentService;
    @Autowired
    private MessageErrorService messageErrorService;
    @Autowired
    private UserService userService;
    @Autowired
    private LogService logService;


    //////////////////////////////////////////////////////////////////////////////////
    /*                               Студент                                        */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список сообщений об ошибке, сформированных авторизованным студентом - для студента")
    @GetMapping("/student/messageError")
    public List<AllMessageErrorOfStudentView> getAllMessageErrorOfStudent() {
        int studentId = studentService.getStudentId();
        return messageErrorService.getAllMessageErrorOfStudent(studentId, AllMessageErrorOfStudentView.class);
    }

    @ApiOperation("Конкретное сообщение об ошибке по id этого сообщения - для студента")
    @GetMapping("/student/messageError/{messageErrorId}")
    public ErrorView getMessageErrorOfStudent(@PathVariable
                                                  @ApiParam(value = "Id сообщения об ошибке. Not null. >0", example = "12")
                                                          int messageErrorId) {
        int studentId = studentService.getStudentId();
        ErrorView messageError =  messageErrorService.getMessageErrorOfStudent(studentId, messageErrorId, ErrorView.class);
        if (messageError == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение об ошибке с указанным id не найдено или принадлежит другому студенту");
        return messageError;
    }

    @ApiOperation("Создание нового сообщения об ошибке - для студента")
    @PostMapping("/student/newMessageError")
    public SuccessfullResponse newMessageError(@RequestBody
                                              @ApiParam(value = "Запрос с данными для создания сообщения об ошибке")
                                                      CreationErrorRequest creationErrorRequest) {
        MessageError messageError = new MessageError();
        messageError.setThemeError(creationErrorRequest.getTheme());
        messageError.setDescriptionError(creationErrorRequest.getDescription());
        messageErrorService.createNewMessageError(messageError);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Сообщение об ошибке успешно отправлено");

    }


    //////////////////////////////////////////////////////////////////////////////////
    /*                                   Админ                                      */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список статусов обработки ошибки - для админа")
    @GetMapping("/admin/statusMessageError")
    public List<StatusErrorView> getStatusMessageError() {
        return  messageErrorService.getAllStatusMessageError(StatusErrorView.class);
    }

    @ApiOperation("Список сообщений об ошибке - для админа")
    @GetMapping("/admin/messageError")
    public List<ErrorsAdminView> getMessagesErrorAdmin(@RequestParam
                                                           @ApiParam (value = "Id статуса обработки сообщения об ошибке. Not null. [1,4]", example = "1")
                                                                   Optional<Integer> statusId) {
        if (statusId.isPresent())
        {
            StatusMessageError statusMessageError = messageErrorService.getStatusMessageError(statusId.get(), StatusMessageError.class);
            if (statusMessageError==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус обработки сообщения об ошибке с указанными id не найден");
            return  messageErrorService.getMessageErrorByStatus(statusId.get(), ErrorsAdminView.class);
        }
        else
            return  messageErrorService.getAllMessageErrorForAdmin(ErrorsAdminView.class);
    }

    @ApiOperation("Конкретное сообщение об ошибке - для админа")
    @GetMapping("/admin/messageError/{errorId}")
    public ErrorAdminView getMessagesErrorAdmin(@PathVariable
                                                    @ApiParam (value = "Id сообщения об ошибке. Not null. >0", example = "21")
                                                            int errorId) {
        ErrorAdminView messageError = messageErrorService.getMessageErrorForAdmin(errorId, ErrorAdminView.class);
        if (messageError==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение об ошибке с указанным id не найдено");
        return messageError;
    }

    @ApiOperation("Изменение комментария к сообщению об ошибке - для админа")
    @PutMapping("/admin/changeCommentMessageError")
    public SuccessfullResponse changeStatusMessageError(@RequestBody
                                                       @ApiParam(value = "Запрос с данными для изменения комментария к сообщению об ошибке")
                                                               ChangeCommentRequest changeCommentRequest) throws MessagingException {
        MessageError messageError = messageErrorService.getMessageErrorForAdmin(changeCommentRequest.getId(), MessageError.class);
        Student findStudent = messageError.getStudentError();
        User findUser = findStudent.getUserForStudent();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение комментария к вашему обращению " + messageError.getThemeError());
        String messageText = "";

        if (messageError==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение об ошибке не найдено");
        String oldData = messageError.getCommentError();
        messageError.setCommentError(changeCommentRequest.getComment());
        messageError = messageErrorService.saveMassageError(messageError);
        messageText += "У вашего обращения новый комментарий от администратора: " + changeCommentRequest.getComment();
        logService.createNewLog(userService.getUserId(), 3, messageError.getIdError(), oldData, messageError.getCommentError());
        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Комментарий успешно изменен");
    }

    @ApiOperation("Изменение статуса обработки сообщения об ошибке -  для админа")
    @PutMapping("/admin/changeStatusMessageError/{messageId}/{statusId}")
    public SuccessfullResponse changeStatusMessageError(@PathVariable
                                                       @ApiParam (value = "Id сообщения об ошибке. Not null. >0", example = "21")
                                                               int messageId,
                                                   @PathVariable
                                                        @ApiParam (value = "Id статуса рассмотрения сообщения об ошибке. Not null. [1,4]", example = "2")
                                                                int statusId) throws MessagingException {
        MessageError messageError = messageErrorService.getMessageErrorForAdmin(messageId, MessageError.class);
        Student findStudent = messageError.getStudentError();
        User findUser = findStudent.getUserForStudent();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение статуса обработки вашего обращения " + messageError.getThemeError());
        String messageText = "";

        if (messageError==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение об ошибке с указанным id не найдено");

        StatusMessageError statusMessageError = messageErrorService.getStatusMessageError(statusId, StatusMessageError.class);
        if (statusMessageError==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус обработки сообщения об ошибке с указанным id не найден");

        String oldData = messageError.getStatusMessageError().getStatusErrorName();
        messageError.setStatusMessageError(statusMessageError);
        messageError = messageErrorService.saveMassageError(messageError);
        String newData = messageError.getStatusMessageError().getStatusErrorName();

        messageText += "У вашего обращения новый статус: " + messageError.getStatusMessageError().getStatusErrorName();
        logService.createNewLog(userService.getUserId(), 4, messageError.getIdError(), oldData, newData);

        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Статус успешно изменен");
    }
}
