package com.example.demo.controller;

import com.example.demo.controller.request.RecoveryPasswordRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.CodeReset;
import com.example.demo.entity.User;
import com.example.demo.service.CodeService;
import com.example.demo.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.UUID;

@Api(description = "Контроллер восстановлений/подтверждений аккаунта")
@RestController
public class RecoveryController {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserService userService;
    @Autowired
    private CodeService codeService;

    @ApiOperation("Отправка почты для восстановления аккаунта/пароля")
    @PostMapping("/code/sendEmail/{email}/{role}/{whatRecovering}")
    public SuccessfullResponse resetPassword(@PathVariable
                                            @ApiParam (value = "Электронная почта, на которую был зарегистрирован аккаунт. Not null", example = "patyphonanik@yandex.ru")
                                                    String email,
                                             @PathVariable
                                        @ApiParam (value = "Роль пользователя в системе. Not null", example = "Admin", allowableValues = "Admin, Moderator, Dekanat")
                                                String role,
                                             @PathVariable
                                            @ApiParam (value = "Что мы восстанавливаем. Not null.", example = "patyphonanik@yandex.ru", allowableValues = "password, account")
                                                    String whatRecovering) {
        //Проверка на валидность введенной электронной почты
        String roleNameForServer;
        EmailValidator emailValidator = EmailValidator.getInstance();
        boolean validEmail = emailValidator.isValid(email);
        if (!validEmail)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введен некореектный Email");

        //Проверка есть ли пользователь с такой почтой
        if (role.equals("Admin"))
            roleNameForServer = "Администратор";
        else if (role.equals("Moderator"))
            roleNameForServer = "Модератор";
        else if (role.equals("Dekanat"))
            roleNameForServer = "Деканат";
        else
            roleNameForServer = "Студент";
        User user = userService.findByEmailAndRoleName(email, roleNameForServer);
        if (user==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с таким email не найден");

        //Проверка на наличие права восстановления аккаунта
        if (user.getStatusUser()==userService.getStatusUser("Аннигилирован")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь удален без права восстановления");
        }

        //Если это восстановление аккаунта, то проверяем что аккаунт был удален
        if (whatRecovering.equals("account") && user.getStatusUser()!=userService.getStatusUser("Удален"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Невозможно восстановить аккаунт, который не был удален");

        //Если это восстановление пароля, то проверяем что аккаунт не был удален
        if (whatRecovering.equals("password") && user.getStatusUser()==userService.getStatusUser("Удален"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Невозможно изменить пароль от аккаунта, который был удален");

        //Проверяем, что whatRecovering принял допустимое значение
        if (!whatRecovering.equals("account") && !whatRecovering.equals("password"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверное значение переменной \"whatRecovering\" ");

        //Если все нормально, отправляем письмо
        sendMailReset(email, roleNameForServer);
        return new SuccessfullResponse(HttpStatus.OK.value(), "На указанную почту отправлено письмо с кодом подтверждения");
    }

    @ApiOperation("Проверка кода для восстановления пароля/аккаунта")
    @PutMapping("/code/reset/{code}/{email}/{role}")
    public SuccessfullResponse resetCode(@PathVariable
                                        @ApiParam (value = "Код, присланный на почту пользователю. Not null. 8 символов")
                                                String code,
                                    @PathVariable
                                        @ApiParam (value = "Электронная почта, на которую зарегестрирован аккаунт. Not null.", example = "qwe123@gmail.com")
                                                String email,
                                    @PathVariable
                                        @ApiParam (value = "Роль пользователя в системе. Not null.", example = "Admin", allowableValues = "Admin, Moderator, Dekanat")
                                                String role) {
        String roleNameForServer;
        if (role.equals("Admin"))
            roleNameForServer = "Администратор";
        else if (role.equals("Moderator"))
            roleNameForServer = "Модератор";
        else if (role.equals("Dekanat"))
            roleNameForServer = "Деканат";
        else
            roleNameForServer = "Студент";
        int userId = userService.findByEmailAndRoleName(email, roleNameForServer).getIdUser();

        if (codeService.getCodeReset(userId).getId().equals(code))
        {
            codeService.deleteCodeReset(codeService.getCodeReset(userId));
            return new SuccessfullResponse(HttpStatus.OK.value(), "Код подтверждения действителен");
        }
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Код не найден");
    }

    @ApiOperation("Смена пароля для доступа к утерянному акаунту")
    @PutMapping("/code/password/{role}")
    public SuccessfullResponse resetCodePassword(@RequestBody RecoveryPasswordRequest recoveryPasswordRequest,
                                            @PathVariable
                                                @ApiParam (value = "Роль пользователя в системе. Not null.", example = "Admin", allowableValues = "Admin, Moderator, Dekanat")
                                                        String role) {
        String roleNameForServer;
        if (role.equals("Admin"))
            roleNameForServer = "Администратор";
        else if (role.equals("Moderator"))
            roleNameForServer = "Модератор";
        else if (role.equals("Dekanat"))
            roleNameForServer = "Деканат";
        else
            roleNameForServer = "Студент";
        User user = userService.findByEmailAndRoleName(recoveryPasswordRequest.getEmail(), roleNameForServer);
        if (user.getPasswordUser().equals(recoveryPasswordRequest.getPassword()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Новый пароль не может совпадать с предыдущим");
        if (recoveryPasswordRequest.getPassword().length()<8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен состоять не менее чем из 8 символов");
        user.setPasswordUser(recoveryPasswordRequest.getPassword());
        userService.savePassword(user);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Пароль успешно изменен");
    }

    @ApiOperation("Восстановление доступа к удаленному аккаунту")
    @PutMapping("/code/account/{email}/{role}")
    public SuccessfullResponse resetCodeAccount(@PathVariable
                                               @ApiParam (value = "Электронная почта, на которую зарегестрирован аккаунт. Not null.", example = "qwe123@gmail.com")
                                                       String email,
                                           @PathVariable
                                           @ApiParam (value = "Роль пользователя в системе. Not null.", example = "Admin", allowableValues = "Admin, Moderator, Dekanat")
                                                   String role) {
        String roleNameForServer;
        if (role.equals("Admin"))
            roleNameForServer = "Администратор";
        else if (role.equals("Moderator"))
            roleNameForServer = "Модератор";
        else if (role.equals("Dekanat"))
            roleNameForServer = "Деканат";
        else
            roleNameForServer = "Студент";
        User user = userService.findByEmailAndRoleName(email, roleNameForServer);
        if (user.getStatusUser()==userService.getStatusUser("Удален"))
            user.setStatusUser(userService.getStatusUser("Активен"));
        else
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Аккаунт имеет статус - "+user.getStatusUser().getStatusUser()+". Аккаунт не может быть восстановлен");
        userService.saveUser(user);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Аккаунт успешно восстановлен");
    }

    //Отправка письма с кодом на почту пользователя
    private void sendMailReset(String email, String role)  {
        try {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            int userId = userService.findByEmailAndRoleName(email, role).getIdUser();
            if (codeService.getCodeReset(userId)!=null)
                codeService.deleteCodeReset(codeService.getCodeReset(userId));
            CodeReset codeReset = new CodeReset();
            codeReset.setId(code);
            codeReset.setUserCodeReset(userService.findByEmailAndRoleName(email, role));
            codeService.saveCodeReset(codeReset);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Код доступа для восстановления аккаунта");
            helper.setText("Сгенерированный код доступа: "+code+" \n" +
                    "Для восстановления доступа к аккаунту скопируйте этот код, перейдите в приложение и вставьте в соответствующее поле ввода.", false);
            mailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
