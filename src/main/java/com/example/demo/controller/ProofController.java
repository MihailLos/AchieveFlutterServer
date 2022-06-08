package com.example.demo.controller;

import com.example.demo.controller.request.ChangeCommentRequest;
import com.example.demo.controller.request.PhotoRequest;
import com.example.demo.controller.request.CreationProofRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.*;
import com.example.demo.view.*;
import com.example.demo.service.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Api(description = "Контроллер подтверждений достижения")
@RestController
public class ProofController {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private ProofService proofService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private FileService fileService;
    @Autowired
    private AchieveService achieveService;
    @Autowired
    private UserService userService;
    @Autowired
    private LogService logService;

    //Ничего не принимает на вход, возвращает список заявок в виде ProofsView (описание там)
    @ApiOperation("Список заявок на подтверждение достижения - для студента")
    @GetMapping ("/student/proof")
    public List<ProofsView> getProofs() {
        int studentId = studentService.getStudentId();
        return proofService.getProofsForStudent(studentId, ProofsView.class);
    }

    //На вход принимает id заявки на подтверждение достижения, возвращает эту заявку в виде ProofView (описание там)
    @ApiOperation("Конкретная заявка на подтверждение достижения - для студента")
    @GetMapping("/student/proof/{proofId}")
    public ProofView getProofs(@PathVariable
                                   @ApiParam(value = "id подтверждения достижения. Not null. >0", example = "1")
                                           int proofId) {
        int studentId = studentService.getStudentId();
        ProofView proof = proofService.getProofStudent(studentId, proofId, ProofView.class);
        if (proof==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Подтверждение с указанным id не найдено или принадлежит другому студенту");
        return proof;
    }

    @ApiOperation("Список файлов-подтвеждений достижения - для студента")
    @GetMapping("/student/listFile/{listFileId}")
    public List<PhotoView> getListFile(@PathVariable
                                           @ApiParam(value = "id листа файлов. Not null. >0", example = "1")
                                                   int listFileId) {
        ListFile listFile = fileService.getListFileById(listFileId, ListFile.class);
        if (listFile==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лист файлов с указанным id не найден");
        return proofService.getFilesForListFile(listFileId, PhotoView.class);
    }

    @ApiOperation("Добавление файлов к подтверждению достижения")
    @PostMapping("/student/newFile")
    public SuccessfullResponse newFile(@RequestBody
                                      @ApiParam(value = "Запрос с данными добавляемого файла")
                                              PhotoRequest request) {
        File file = new File();
        file.setDataFile(request.getData());
        file.setFormatFile(request.getFormat());
        file.setListFile(fileService.getListFileById(request.getListFileId(), ListFile.class));
        fileService.saveFile(file);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Файл успешно добавлен к подтверждению достижения");
    }

    @ApiOperation("Новая заявка на подтверждение достижения - для студента")
    @PostMapping("/student/newProof")
    public ResponseEntity newProof(@RequestBody
                                       @ApiParam(value = "Запрос с данными подтверждения достижения")
                                           CreationProofRequest creationProofRequest) {
        AchievementOfStudent achievementOfStudentTest = achieveService.getReceivedAchieveForStudent(creationProofRequest.getAchieveId(), studentService.getStudentId(), AchievementOfStudent.class);
        if (achievementOfStudentTest!=null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Данное достижение уже было получено");
        ProofAchieve proofAchieveTest = proofService.getProofByStudentAndAchieve(creationProofRequest.getAchieveId(), studentService.getStudentId(), 2, ProofAchieve.class);
        if (proofAchieveTest!=null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заявка на получение данного достижения уже была отправлена");
        Achievement achievementEndDateTest = achieveService.getAchieveById(creationProofRequest.getAchieveId(), Achievement.class);
        LocalDate todayDate = java.time.LocalDate.now();
        if (achievementEndDateTest.getEndDateAchieve() != null) {
            if (todayDate.isAfter(achievementEndDateTest.getEndDateAchieve()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Срок действия достижения истёк");
        }

        ProofAchieve proofAchieve = new ProofAchieve();
        proofAchieve.setDescriptionProof(creationProofRequest.getDescription());
        Student student = studentService.getStudentById(studentService.getStudentId(), Student.class);
        proofAchieve.setStudentProof(student);
        Achievement achievement = achieveService.getAchieveById(creationProofRequest.getAchieveId(), Achievement.class);
        proofAchieve.setAchievementForProof(achievement);
        proofAchieve.setDateProof(LocalDate.now());
        proofAchieve.setCommentProof(null);

        ListFile listFile;
        if (creationProofRequest.getFiles()!=0)
        {
            listFile = fileService.newListFile();
            proofAchieve.setListFileForProof(listFile);
            proofService.newProof(proofAchieve);
            return  ResponseEntity.status(HttpStatus.OK).body(listFile.getIdListFile());
        }
        proofAchieve.setListFileForProof(null);
        proofService.newProof(proofAchieve);
        return  ResponseEntity.status(HttpStatus.OK).body(0);
    }


    //////////////////////////////////////////////////////////////////////////////////
    /*                                Модератор                                     */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список подтверждений достижений - для модера")
    @GetMapping ("/moderator/proof")
    public List<ProofsAdminView> getProofsModer(@RequestParam
                                                    @ApiParam(value = "Id статуса рассмотрения заявки. Может не передаваться, тогда вернет все заявки. Если передается - Not null. [1,4]", example = "2")
                                                Optional<Integer> statusId) {
        User currentUser = userService.getUser();
        Moderator currentModerator = userService.getModeratorByUserId(currentUser.getIdUser(), Moderator.class);

        if (statusId.isPresent())
            return proofService.getProofByStatus(statusId.get(), currentModerator, ProofsAdminView.class);
        else
            return proofService.getAllProofsForModer(currentModerator, ProofsAdminView.class);
    }

    @ApiOperation("Конкретная заявка на подтверждение достижения - для админа/модера")
    @GetMapping ("/moderator/proof/{proofId}")
    public ProofAdminView getProof(@PathVariable
                                       @ApiParam(value = "Id заявки на подтверждение достижения. Not null. >0", example = "1")
                                           int proofId) {
        //Проверяем есть ли у нас заявка с таким id
        ProofAchieve proofAchieve = proofService.getProof(proofId, ProofAchieve.class);
        if (proofAchieve!=null)
        {
            //Если статус заявки "В обработке". То при открытии заявки меняем статус на "Просмотрено"
            if (proofAchieve.getStatusRequestProof().getStatusRequestName().equals((proofService.getStatusRequest(4, StatusRequest.class)).getStatusRequestName()))
            {
                String oldData = proofAchieve.getStatusRequestProof().getStatusRequestName();
                proofAchieve.setStatusRequestProof(proofService.getStatusRequest(3,StatusRequest.class));
                proofService.saveProof(proofAchieve);
                logService.createNewLog(userService.getUserId(), 11, proofAchieve.getIdProof(), oldData, proofAchieve.getStatusRequestProof().getStatusRequestName());
            }
        }
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка на подтверждение достижения с указанным id не найдена");

        //Возвращаем данные заявки
        return proofService.getProof(proofId, ProofAdminView.class);
    }

    @ApiOperation("Изменение комментария к к подтверждению достижения - для модератора")
    @PutMapping ("/moderator/changeCommentProof")
    public SuccessfullResponse changeCommentProof(@RequestBody
                                                   @ApiParam(value = "Запрос с данными для изменения комментария к подтверждению достижения")
                                                           ChangeCommentRequest changeCommentRequest) throws MessagingException {
        ProofAchieve proofAchieve = proofService.getProof(changeCommentRequest.getId(), ProofAchieve.class);
        Student findStudent = proofAchieve.getStudentProof();
        User findUser = findStudent.getUserForStudent();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение комментария к подтверждению достижения " + proofAchieve.getAchievementForProof().getNameAchieve());
        String messageText = "";
        if (proofAchieve==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Подтверждение достижения не найдено");
        String oldData = proofAchieve.getCommentProof();
        proofAchieve.setCommentProof(changeCommentRequest.getComment());
        messageText += "Комментарий от модератора к Вашему подтверждению достижения: " + changeCommentRequest.getComment();
        proofService.saveProof(proofAchieve);
        logService.createNewLog(userService.getUserId(), 3, proofAchieve.getIdProof(), oldData, proofAchieve.getCommentProof());
        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Комментарий успешно изменен");
    }

    @ApiOperation("Список статусов рассмотрения заявки на подтверждение достижения, которые может присвоить заявке модератор")
    @GetMapping ("/moderator/statusRequest")
    public List<StatusRequestView> getStatusRequest() {
        List<StatusRequestView> statusRequests = new ArrayList<>();
        //Подтверждено
        statusRequests.add(proofService.getStatusRequest(1, StatusRequestView.class));
        //Отклонено
        statusRequests.add(proofService.getStatusRequest(2, StatusRequestView.class));
        //Просмотрено
        statusRequests.add(proofService.getStatusRequest(3, StatusRequestView.class));
        //В обработке
        statusRequests.add(proofService.getStatusRequest(4, StatusRequestView.class));
        return statusRequests;
    }

    @ApiOperation("Изменение статуса обработки подтверждения достижения - для админа/модера")
    @PutMapping ("/moderator/proof/{proofId}/{statusId}")
    public SuccessfullResponse changeStatusProof(@PathVariable
                                                @ApiParam(value = "Id заявки на подтверждение достижения. Not null. >0", example = "13")
                                                        int proofId,
                                            @PathVariable
                                                @ApiParam(value = "Id статуса обработки подтверждения достижения. Not null. [1,2]", example = "2")
                                                        int statusId) throws MessagingException {
        //Проверяем есть ли у нас заявка с таким id
        ProofAchieve proofAchieve = proofService.getProof(proofId, ProofAchieve.class);
        Student findStudent = proofAchieve.getStudentProof();
        User findUser = findStudent.getUserForStudent();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение статуса обработки подтверждения достижения " + proofAchieve.getAchievementForProof().getNameAchieve());
        String messageText = "";
        if (proofAchieve!=null)
        {
            //Проверяем есть ли у студента достижение, на получение которого он подал заявку
            AchievementOfStudent achievementTest = achieveService.getAchieveForStudent(proofAchieve.getAchievementForProof().getIdAchieve(), proofAchieve.getStudentProof().getIdStudent(), AchievementOfStudent.class);
            if (achievementTest!=null && proofAchieve.getStatusRequestProof().getIdStatusRequest()!=2)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Студент уже получил данное достижение! Заявка автоматически отклонена");

            //Проверяем, была ли эта заявка уже подтверждена
            if (proofAchieve.getStatusRequestProof().getIdStatusRequest()==1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя изменить статус уже подтвержденной заявки");
            }
            //Если нет, то меняем статус
            String oldData = proofAchieve.getStatusRequestProof().getStatusRequestName();
            proofAchieve.setStatusRequestProof(proofService.getStatusRequest(statusId, StatusRequest.class));
            proofService.saveProof(proofAchieve);
            messageText += "Модератор изменил статус обработки Вашего подтверждения достижения: " + proofService.getStatusRequest(statusId, StatusRequest.class).getStatusRequestName();
            //Если модератор подтверждает заявку, то добавляем это достижение в полученные достижения
            if (statusId==1) {
                AchievementOfStudent achievementOfStudent = new AchievementOfStudent();
                achievementOfStudent.setAchievementForAchieveOfStudent(achieveService.getAchieveById(proofAchieve.getAchievementForProof().getIdAchieve(), Achievement.class));
                achievementOfStudent.setStudentForAchieveOfStudent(studentService.getStudentById(proofAchieve.getStudentProof().getIdStudent(), Student.class));
                achievementOfStudent.setStatusRewardForAchieveOfStudent(false);
                messageText += "Вы получили данное достижение! Поздравляем! Не забудьте подтвердить получение награды в личном профиле.";
                achieveService.saveAchievementOfStudent(achievementOfStudent);
                logService.createNewLog(userService.getUserId(), 10, achievementOfStudent.getIdAchieveOfStudent(), null, achievementOfStudent.getAchievementForAchieveOfStudent().getNameAchieve());
            }
            logService.createNewLog(userService.getUserId(), 11, proofAchieve.getIdProof(), oldData, proofAchieve.getStatusRequestProof().getStatusRequestName());
            helper.setText(messageText);
            mailSender.send(notificationMessage);
            return new SuccessfullResponse(HttpStatus.OK.value(), "Статус заявки успешно изменен");
        }
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка на подтверждение достижения с указанным id не найдена");
    }
}
