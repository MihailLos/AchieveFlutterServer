package com.example.demo.controller;

import com.example.demo.controller.request.CreationBanRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.*;
import com.example.demo.service.*;
import com.example.demo.view.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(description = "Контроллер пользователя")
@RestController
public class UserController {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserService userService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private BanService banService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private LogService logService;

    @ApiOperation("Изменение email")
    @PutMapping("/change/email/{email}")
    public SuccessfullResponse changeEmail(@PathVariable
                                          @ApiParam(value = "Новая электронная почта. Not null", example = "patyphonanik@yandex.ru")
                                                  String email) {
        //Получаем текущего пользователя
        User user = userService.findUser(userService.getUserId());

        //Проверка на совпадение почты с текущей
        if (user.getEmailUser().equals(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Новый email не может совпадать с предыдущим");

        //Проверка на наличие зарегистрированного пользователя с указанной электронной почтой
        User test = userService.findByEmail(email);
        if (test != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует!");

        //Проверка на валидность введенной электронной почты
        EmailValidator emailValidator = EmailValidator.getInstance();
        boolean validEmail = emailValidator.isValid(email);
        if (!validEmail)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введен некорректный Email");

        //Если все проверки прошли, менияем данные и сохраняем
        user.setEmailUser(email);
        userService.saveUser(user);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Email успешно изменен");
    }

    @ApiOperation("Email авторизованного пользователя")
    @GetMapping("student/getEmail")
    public String getUserEmail() {
        return userService.findUser(userService.getUserId()).getEmailUser();
    }

    @ApiOperation("Список модераторов для админа")
    @GetMapping("admin/getModerators")
    public List <ModersView> getModerators() {
        return userService.getAllModerators(ModersView.class);
    }

    @ApiOperation("Назначение институтов модератору для админа")
    @PutMapping("admin/addInstitutesForModerator/{userId}")
    public SuccessfullResponse addInstitutesForModerator(
            @PathVariable int userId,
            @RequestParam
            @ApiParam(value = "Список id добавляемых институтов. Not Null", example = "1,2,3")
                    Optional<List<Integer>> listOfInstitutesId
    ) throws MessagingException {
        //Проверяем есть ли модератор с таким id
        Moderator moderator = userService.getModeratorByUserId(userId, Moderator.class);
        User findUser = moderator.getUser();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Вам назначили новые институты");
        String messageText = "";
        if (moderator == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Модератор с указанным id не найден");

        if (listOfInstitutesId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Список институтов пуст!");
        }

        Set<String> nameInstitutes = new HashSet<>();
        Set<Institute> institutePool = new HashSet<>();
        if (!listOfInstitutesId.get().isEmpty()) {
            for (Integer instituteId : listOfInstitutesId.get()) {
                institutePool.add(educationService.getInstitute(instituteId));
                nameInstitutes.add(educationService.getInstitute(instituteId).getInstituteName());
            }

            messageText += "Вам были назначены следующие институты: " + nameInstitutes;
            moderator.setInstitutes(institutePool);
            userService.saveModer(moderator);
        }

        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Модератору успешно изменен список институтов");
    }

//    @ApiOperation("Удаление институтов у модератора для админа")
//    @DeleteMapping("admin/deleteInstitutesFromModerator/{moderatorId}")
//    public SuccessfullResponse deleteInstitutesFromModerator(
//            @PathVariable int moderatorId,
//            @RequestParam
//            @ApiParam(value = "Список id удаляемых институтов. Not Null", example = "1,2,3")
//                    Optional<List<Integer>> listOfInstitutesId
//    ) throws MessagingException {
//        //Проверяем есть ли модератор с таким id
//        Moderator moderator = userService.getModerator(moderatorId, Moderator.class);
//        User findUser = moderator.getUser();
//        String userEmail = findUser.getEmailUser();
//        MimeMessage notificationMessage = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
//        helper.setTo(userEmail);
//        helper.setSubject("Вас отвязали от некоторых институтов");
//        String messageText = "";
//
//        if (moderator == null)
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Модератор с указанным id не найден");
//
//        if (listOfInstitutesId.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Список институтов пуст!");
//        }
//
//        Set<String> nameInstitutes = new HashSet<>();
//        List<Integer> inputInstituteList = listOfInstitutesId.get();
//        for (Integer instituteId : inputInstituteList) {
//            Institute removableInstitute = educationService.getInstitute(instituteId);
//            nameInstitutes.add(educationService.getInstitute(instituteId).getInstituteName());
//            moderator.removeInstitute(removableInstitute);
//        }
//
//        messageText += "Вы были отвязаны от следующих институтов: " + nameInstitutes;
//
//        userService.saveModer(moderator);
//
//        helper.setText(messageText);
//        mailSender.send(notificationMessage);
//        return new SuccessfullResponse(HttpStatus.OK.value(), "Институты успешно удалены");
//    }


    @ApiOperation("Список пользователей (по роли или всех) - для админа")
    @GetMapping("admin/getUsers")
    public List <UsersView> getUsers(@RequestParam
                                         @ApiParam(value = "Id роли пользователя. Фильтр, может не передаваться. Если передается - Not null. [1,3]", example = "1")
                                                 Optional<Integer> roleId,
                                     @RequestParam
                                     @ApiParam(value = "Id статуса пользователя. Фильтр, может не передаваться. Если передается - Not null. [1,4]", example = "3")
                                             Optional<Integer> statusId,
                                     @RequestParam
                                     @ApiParam(value = "Подстрока с фамилией(или ее частью) пользователя, которого ищем. Фильтр, может не передаваться. Если передается - Not null", example = "КИсл")
                                             Optional<String > substring) {
        if (roleId.isEmpty()) {
            if (substring.isEmpty()) {
                if (statusId.isEmpty())
                    return userService.getAllUser(UsersView.class);
                else
                    return userService.getUserByStatus(statusId.get(), UsersView.class);
            }
            else {
                if (statusId.isEmpty())
                    return userService.getUserBySubstring(substring.get(), UsersView.class);
                else
                    return userService.getUserBySubstringAndStatus(substring.get(), statusId.get(), UsersView.class);
            }
        }
        else {
            if (substring.isEmpty())
            {
                if (statusId.isEmpty())
                    return userService.getUserByRole(roleId.get(), UsersView.class);
                else
                    return userService.getUserByRoleAndStatus(roleId.get(), statusId.get(), UsersView.class);
            }
            else
            {
                if (statusId.isEmpty())
                    return userService.getUserBySubstringAndRole(substring.get(), roleId.get(), UsersView.class);
                else
                    return userService.getUserBySubstringAndRoleAndStatus(substring.get(), roleId.get(), statusId.get(), UsersView.class);
            }
        }
    }

    @ApiOperation("Конкретный пользователь - для админа")
    @GetMapping("/admin/getUser/{userId}")
    public UserView getUserByIdForAdmin(@PathVariable
                                            @ApiParam(value = "id пользователя Not null. >0", example = "12")
                                                    int userId) {
        UserView user = userService.getUserById(userId, UserView.class);
        //Проверяем, есть ли у нас пользователь с таким id
        if(user!=null)
            return user;
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
    }

    @ApiOperation("Удаление пользователя без права восстановления - для админа")
    @PutMapping("/admin/deleteUser/{userId}")
    public SuccessfullResponse deleteUserByIdForAdmin(@PathVariable
                                                     @ApiParam(value = "id пользователя. Not null. >0", example = "16")
                                                             int userId) throws MessagingException {
        User user = userService.getUserById(userId, User.class);
        String userEmail = user.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Ваш аккаунт удален!");
        helper.setText("Ваш аккаунт в информационной системе учета достиженй студентов удален без права восстановления!");
        //Если пользователь найден, то меняем его статус
        if(user!=null) {
            String oldData = user.getStatusUser().getStatusUser();
            user.setStatusUser(userService.getStatusUser("Аннигилирован"));
            userService.saveUser(user);
            logService.createNewLog(userService.getUserId(), 15, user.getIdUser(), oldData, user.getStatusUser().getStatusUser());
            mailSender.send(notificationMessage);
            return new SuccessfullResponse(HttpStatus.OK.value(), "Пользователь удален без права на восстановление");
        }
        //Иначе выводим соответствующее сообщение
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
    }

    @ApiOperation("Бан студента")
    @PutMapping("/moderator/bannedUser")
    public SuccessfullResponse bannedUser(@RequestBody
                                         @ApiParam(value = "Запрос с данными для создания бана")
                                                 CreationBanRequest creationBanRequest) throws MessagingException {

        //Проверяем есть ли такой студент
        Student student = studentService.getStudentById(creationBanRequest.getStudentId(), Student.class);
        if (student==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");

        //Проверяем смогли ли мы получить для него пользователя
        int userId = student.getUserForStudent().getIdUser();
        User user = userService.findUser(userId);
        String userEmail = user.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Ваш аккаунт забанен!");
        String messageText = "Ваш аккаунт в информационной системе учета достижений студентов забанен!\n";

        if (user==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");

        //Если все хорошо, записываем статус и сохраняем пользователя
        String oldData = user.getStatusUser().getStatusUser();
        user.setStatusUser(userService.getStatusUser("Забанен"));
        logService.createNewLog(userService.getUserId(), 15, user.getIdUser(), oldData, user.getStatusUser().getStatusUser());
        userService.saveUser(user);

        //Создаем бан и сохраняем
        Ban ban = new Ban();
        ban.setDateEndBan(creationBanRequest.getBanDateEnd());
        messageText = "Дата окончания бана: " + creationBanRequest.getBanDateEnd().toString() + "\n";
        ban.setCreatorBan(userService.findUser(userService.getUserId()));
        ban.setReasonBan(creationBanRequest.getBanReason());
        messageText = "Причина бана: " + creationBanRequest.getBanReason() + "\n";
        ban.setUserBanned(userService.findUser(userId));
        banService.saveBan(ban);

        helper.setText(messageText);
        mailSender.send(notificationMessage);

        logService.createNewLog(userService.getUserId(), 14, ban.getIdBan(), null, "userId="+user.getIdUser()+" - "+ban.getReasonBan()+" "+ban.getDateEndBan());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Студент забанен");
    }

    @ApiOperation("Список ролей для админа")
    @GetMapping ("/admin/getRoles")
    public List<RoleView> getTypeOperation() {
        return userService.getAllRoles(RoleView.class);
    }
}
