package com.example.demo.controller;

import com.example.demo.controller.request.*;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.*;
import com.example.demo.view.*;
import com.example.demo.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.*;

@Api(description = "Контроллер достижений")
@RestController
public class AchievementController {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private FileService fileService;
    @Autowired
    private ProofService proofService;
    @Autowired
    private AchieveService achieveService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private UserService userService;
    @Autowired
    private RewardService rewardService;
    @Autowired
    private LogService logService;;

    //Список id статусов активности достижений, которые не отображаются для студента
    private List<Integer> getStatusIds() {
        List<Integer> status = new ArrayList<>();
        status.add(1);
        status.add(5);
        status.add(6);
        return status;
    }

    @ApiOperation("Список категорий достижений")
    @GetMapping("/categories")
    public List<CategoriesView> getAllCategories() {
        //Возвращаем список всех категорий
        return achieveService.getAllCategory(CategoriesView.class);
    }

    @ApiOperation("Получение конкретной категории достижения")
    @GetMapping("/categories/{categoryId}")
    public CategoriesView getCategory(
            @PathVariable
            @ApiParam(value = "Id категории. Not null. >0", example = "3")
                    int categoryId
    ) {
        //Возвращаем категорию по ее айди
        return achieveService.getCategory(categoryId, CategoriesView.class);
    }

    @ApiOperation("Изменение данных о категории достижений - для админа")
    @PutMapping("/admin/changeCategory/{categoryId}")
    public SuccessfullResponse changeCategory(@PathVariable
                                       @ApiParam(value = "Id категории. Not null. >0", example = "3")
                                               int categoryId,
                                              @RequestBody
                                       @ApiParam(value = "Тело с изменяемыми параметрами")
                                              ChangeCategoryRequest changeCategoryRequest) {
        Category category = achieveService.getCategory(categoryId);
        if (category==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория с указанным id не найдена");

        String oldData="";

        //Если название награды было передано, меняем его
        if (!changeCategoryRequest.getCategoryName().isEmpty())
        {
            oldData=changeCategoryRequest.getCategoryName().toString();
            category.setNameCategory(changeCategoryRequest.getCategoryName().get());
        }
        //Если изображение или его формат были переданы, получаем файл и меняем указанные данные
        if (!changeCategoryRequest.getFileData().isEmpty() || !changeCategoryRequest.getFileFormat().isEmpty()) {
            int fileId = category.getFileOfCategory().getIdFile();
            File file = fileService.getFileById(fileId, File.class);
            if (file==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл с иконкой награды не найден");
            if (!changeCategoryRequest.getFileData().isEmpty())
            {
                if (oldData.equals(""))
                    oldData=String.valueOf(changeCategoryRequest.getFileData());
                else
                    oldData=oldData+" "+ Arrays.toString(changeCategoryRequest.getFileData().get());
                file.setDataFile(changeCategoryRequest.getFileData().get());
            }
            if (!changeCategoryRequest.getFileFormat().isEmpty())
            {
                if (oldData.equals(""))
                    oldData=changeCategoryRequest.getFileFormat().get();
                else
                    oldData=oldData+" "+changeCategoryRequest.getFileFormat().get();
                file.setFormatFile(changeCategoryRequest.getFileFormat().get());
            }
            fileService.resetFile(file);
        }
        achieveService.saveCategory(category);
        logService.createNewLog(userService.getUserId(), 13, category.getIdCategory(), oldData, category.getNameCategory());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Данные успешно изменены");
    }

    @ApiOperation("Удаление категории достижений - для админа")
    @DeleteMapping("/admin/deleteCategory/{categoryId}")
    public SuccessfullResponse deleteCategory(
            @PathVariable
            @ApiParam(value = "Id категории. Not null. >0", example = "3") int categoryId
    ) {
        List<Achievement> findAchievement = achieveService.getAchieveByCategory(categoryId, Achievement.class);

        if (!findAchievement.isEmpty()) {
            Map<Integer, String> achievementsNameList = new HashMap<>();

            for(Achievement achievement: findAchievement) {
                achievementsNameList.put(achievement.getIdAchieve(), achievement.getNameAchieve());
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Невозможно удалить награду, которая используется в достижениях: " + achievementsNameList);
        }
        Category findCategory = achieveService.getCategory(categoryId);
        File categoryPicture = findCategory.getFileOfCategory();

        achieveService.deleteCategory(findCategory);
        fileService.deleteFile(categoryPicture);

        return new SuccessfullResponse(HttpStatus.OK.value(), "Категория успешно удалена");
    }

    @ApiOperation("Создание новой категории - для админа")
    @PostMapping("/admin/createCategory")
    public SuccessfullResponse createCategory(@RequestBody CreationCategoryRequest creationCategoryRequest) {
        Category category = new Category();
        Category testCategory = achieveService.getCategoryByName(creationCategoryRequest.getCategoryName());

        if (testCategory != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Категория с таким названием уже существует");
        }

        category.setNameCategory(creationCategoryRequest.getCategoryName());

        File file = new File();
        file.setDataFile(creationCategoryRequest.getData());
        file.setFormatFile(creationCategoryRequest.getFormat());
        file.setListFile(null);
        fileService.saveFile(file);

        category.setFileOfCategory(file);
        achieveService.saveCategory(category);
        logService.createNewLog(userService.getUserId(), 12, category.getIdCategory(), null, category.getNameCategory());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Категория успешно создана");
    }

    //////////////////////////////////////////////////////////////////////////////////
    /*                               Студент                                        */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список id статусов активности достижений для студента")
    @GetMapping("/student/statusActives")
    private List<StatusActive> getStatusIdsForStudent() {
        List<StatusActive> statusActives = new ArrayList<>();
        statusActives.add(achieveService.getStatusActive(2));
        statusActives.add(achieveService.getStatusActive(3));
        statusActives.add(achieveService.getStatusActive(4));
        return statusActives;
    }

    @ApiOperation("Список полученных достижений авторизованным или другим студентом по id статуса активности - для студента")
    @GetMapping("/student/achievementsReceived/{statusId}")
    public List<AchievementsReceivedView> getAllReceived(@PathVariable
                                                             @ApiParam(value = "Id статуса активности достижения. Not null. [2, 4]", example = "2", required = true)
                                                                     int statusId,
                                                         @RequestParam
                                                             @ApiParam(value = "Id категории достижения. Фильтр, может не передаваться. Если передается - Not null. [1, 5]", example = "5")
                                                                     Optional<Integer> categoryId,
                                                         @RequestParam
                                                             @ApiParam(value = "Id студента. Может не передаваться. Если не передается - берется id текущего авторизованного студента", example = "7")
                                                                     Optional<Integer> studentId) {
        //Проверяем что id статуса активности передан корректно
        int tempStudentId;
        if (statusId<2 || statusId>4)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус с указанным id не найден");

        //Если id - null, получаем id авторизованного студента
        if(studentId.isEmpty()) {
            tempStudentId = studentService.getStudentId();
        } else {
            tempStudentId = studentId.get();
        }

        //Если id категории не указан, возвращаем полученные достижения из всех категорий
        if(categoryId.isEmpty())
            return achieveService.getAchievementsReceived(tempStudentId, statusId, AchievementsReceivedView.class);
        //Если id категории указан, возвращаем полученные достижения только из этой категории
        else
        {
            //Проверяем что id категории передан корректно
            if (categoryId.get()<1 || categoryId.get()>5)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория с указанным id не найдена");
            return achieveService.getAchievementsReceivedCategory(categoryId.get(), tempStudentId, statusId, AchievementsReceivedView.class);
        }
    }

    @ApiOperation("Данные конкретного полученного достижения по его id - для студента")
    @GetMapping("/student/achievementReceived/{achieveId}")
    public AchievementReceivedView getReceived(@PathVariable
                                                   @ApiParam(value = "Id полученного достижения. Not null. >0", example = "1", required = true)
                                                           int achieveId) {
        //Получаем id автирозованного студента
        int studentId = studentService.getStudentId();

        //Получаем полученное достижение для этого студента
        AchievementReceivedView receivedAchieve = achieveService.getAchieveForStudent(achieveId, studentId, AchievementReceivedView.class);

        //Если достижение найдено, возвращаем его данные
        if(receivedAchieve!=null)
            return receivedAchieve;
        //Если достижение не найдено, возвращаем сообщение об ошибке
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение не найдено или принадлежит другому студенту");
    }

    @ApiOperation("Список неполученных достижений авторизованным студентом по id статуса активности - для студента")
    @GetMapping("/student/achievementsUnreceived/{statusId}")
    public List<AchievementsView> getAllUnreceived(@PathVariable
                                                             @ApiParam(value = "Id статуса активности достижения. Not null. [2,4]", example = "2", required = true)
                                                                     int statusId,
                                                         @RequestParam
                                                             @ApiParam(value = "Id категории достижения. Фильтры, может не передаваться. Если передается - Not null. [1,5]", example = "3")
                                                                     Optional<Integer> categoryId ) {
        //Проверяем что id статуса активности передан корректно
        if (statusId<2 || statusId>4)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус с указанным id не найден");

        //Список полученных достижений
        List<AchievementOfStudent> receivedAchieves;
        //Список заявок на получение достижений
        List<ProofAchieve> proofAchieves;
        //Список id достижений соответствующих полученным достижениям
        List<Integer> achievesIds = new ArrayList<>();

        //Список неполученных достижений
        List<AchievementsView> unreceivedAchieves = new ArrayList<>();
        List<AchievementsView> toRemoveUnreceivedAchieves = new ArrayList<>();

        //Получаем id авторизованного студента
        int studentId = studentService.getStudentId();

        proofAchieves = proofService.getProofsByStudentAndStatusNot(studentId, 2, ProofAchieve.class);
        System.out.println(proofAchieves.size());

        //Если категория не указана, то работаем с достижениями всех категорий
        if(categoryId.isEmpty())
        {
            //Получаем список полученных авторизованным студентом достижений
            receivedAchieves = achieveService.getAchievementsReceived(studentId, statusId, AchievementOfStudent.class);
            //Записываем id в список
            for(AchievementOfStudent achReceived : receivedAchieves)
                achievesIds.add(achReceived.getAchievementForAchieveOfStudent().getIdAchieve());
            //Если у студента нет полученных достижений, возвращаем весь список достижений в системе
            if (achievesIds.size() == 0) {
                unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedAll(getStatusIds(), statusId, AchievementsView.class));
                for (AchievementsView unrecvAchieve: unreceivedAchieves) {
                    for (ProofAchieve proofAchieve: proofAchieves) {
                        Achievement unreceivedAchievement = achieveService.getAchieveById(unrecvAchieve.getId(), Achievement.class);
                        Achievement proofAchievement = proofAchieve.getAchievementForProof();
                        if (unreceivedAchievement == proofAchievement) {
                            toRemoveUnreceivedAchieves.add(unrecvAchieve);
                        }
                    }
                }
                unreceivedAchieves.removeAll(toRemoveUnreceivedAchieves);
            }
            //Если у студента есть полученные достижения, то берем из достижений системы только те, которые студент еще не получил
            else {
                unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedWithoutReceived(getStatusIds(), achievesIds, statusId, AchievementsView.class));
                for (AchievementsView unrecvAchieve: unreceivedAchieves) {
                    for (ProofAchieve proofAchieve: proofAchieves) {
                        Achievement unreceivedAchievement = achieveService.getAchieveById(unrecvAchieve.getId(), Achievement.class);
                        Achievement proofAchievement = proofAchieve.getAchievementForProof();
                        if (unreceivedAchievement.equals(proofAchievement)) {
                            toRemoveUnreceivedAchieves.add(unrecvAchieve);
                        }
                    }
                }
                unreceivedAchieves.removeAll(toRemoveUnreceivedAchieves);
            }
        }
        //Если категория указана, то работаем с достижениями только этой категории
        else
        {
            //Проверяем что id категории передан корректно
            if (categoryId.get()<1 || categoryId.get()>5)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория с указанным id не найдена");
            //Получем список полченных авторизованным студентом достижений
            receivedAchieves = achieveService.getAchievementsReceivedCategory(categoryId.get(), studentId, statusId, AchievementOfStudent.class);
            //Записываем id в список
            for(AchievementOfStudent achReceived : receivedAchieves)
                achievesIds.add(achReceived.getAchievementForAchieveOfStudent().getIdAchieve());
            //Если у студента нет полученных достижений, возвращаем весь список достижений в системе
            if (achievesIds.size() == 0) {
                unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedCategory(getStatusIds(), categoryId.get(), statusId, AchievementsView.class));
                for (AchievementsView unrecvAchieve: unreceivedAchieves) {
                    for (ProofAchieve proofAchieve: proofAchieves) {
                        Achievement unreceivedAchievement = achieveService.getAchieveById(unrecvAchieve.getId(), Achievement.class);
                        Achievement proofAchievement = proofAchieve.getAchievementForProof();
                        if (unreceivedAchievement.equals(proofAchievement)) {
                            toRemoveUnreceivedAchieves.add(unrecvAchieve);
                        }
                    }
                }
                unreceivedAchieves.removeAll(toRemoveUnreceivedAchieves);
            }
            //Если у студента есть полученные достижения, то берем из достижений системы только те, которые студент еще не получил
            else {
                unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedCategoryWithoutReceived(getStatusIds(), achievesIds, categoryId.get(), statusId, AchievementsView.class));
                for (AchievementsView unrecvAchieve: unreceivedAchieves) {
                    for (ProofAchieve proofAchieve: proofAchieves) {
                        Achievement unreceivedAchievement = achieveService.getAchieveById(unrecvAchieve.getId(), Achievement.class);
                        Achievement proofAchievement = proofAchieve.getAchievementForProof();
                        if (unreceivedAchievement.equals(proofAchievement)) {
                            toRemoveUnreceivedAchieves.add(unrecvAchieve);
                        }
                    }
                }
                unreceivedAchieves.removeAll(toRemoveUnreceivedAchieves);
            }
        }
        //Возвращаем список неполученных авторизованным студентом достижений
        return unreceivedAchieves;
    }

    @ApiOperation("Данные конкретного неполученного достижения по его id - для студента")
    @GetMapping("/student/achievementUnreceived/{unreceivedAchieveId}")
    public AchievementView getAchievementUnreceived(@PathVariable
                                                        @ApiParam(value = "Id достижения в системе. Not null. >0", example = "9")
                                                                int unreceivedAchieveId) {
        //Получаем достижение системы по его id
        AchievementView achievementUnreceived = achieveService.getAchieveById(unreceivedAchieveId, AchievementView.class);

        //Если достижение найдено, то возвращаем его данные
        if(achievementUnreceived!=null)
            return achievementUnreceived;
        //Если достижения не найдено, возвращаем ссобщение об ошибке
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение с указанным id не найдено");
    }

    @ApiOperation("Процент выполненных достижений - для студента")
    @GetMapping("/student/achievementsPercent")
    public int getPercent(@RequestParam
                              @ApiParam(value = "id студента, чей профиль мы просматриваем. Не передается, если студент смотрит свой профиль. Если чужой профиль, то Not null. >0", example = "3")
                                      Optional<Integer> studentId) {
        //Если id судента не передан, то записываем в переменную id авторихованного студента
        if (studentId.isEmpty())
            studentId = Optional.of(studentService.getStudentId());
        else {
            //Провряем есть ли в базе студент с указанным id
            Student studentTest = studentService.getStudentById(studentId.get(), Student.class);
            if (studentTest==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент с указанным id не найден");
        }
        //Получаем список всех достижений в системе, которые доступны студентам
        List<AchievementsView> allAchieves = achieveService.getAllAchievesForStudent(getStatusIds(), AchievementsView.class);
        //Получаем список полученных пользователем достижений
        List<AchievementsReceivedView> receivedAchieves = achieveService.getReceivedAchievesForStudent(studentId.get(), AchievementsReceivedView.class);

        //Делим количество полученных достижений на количество всех доступных достижений и умножаем на 100. Возвращаем процент выполненных достижений целым числом
        return (int)(100*((double)receivedAchieves.size()/(double) allAchieves.size()));
    }

    @ApiOperation("Получение награды у полученного достижения по id этого полученного достижения - для студента")
    @PutMapping("/student/getReward/{achieveId}")
    public SuccessfullResponse getReward(@PathVariable
                                        @ApiParam(value = "Id полученного достижения. Not null, >0", example = "11")
                                                int achieveId) {
        //Получаем id авторизованного студента
        int studentId = studentService.getStudentId();
        Student findStudent = studentService.getStudentById(studentId, Student.class);
        int currentStudentScore = findStudent.getScoreStudent();
        //Получаем достижение по указанному id
        AchievementOfStudent achieveOfStudent = achieveService.getReceivedAchieveForStudent(achieveId, studentId, AchievementOfStudent.class);
        Achievement findAchievement = achieveService.getAchieveById(achieveId, Achievement.class);
        //Если достижения найдено, меняем статус награды на true (Получено)
        if(achieveOfStudent!=null) {
            findStudent.setScoreStudent(currentStudentScore + findAchievement.getScoreAchieve());
            studentService.saveStudent(findStudent);
            return achieveService.changeStatusReward(achieveOfStudent);
        }
        //Если достижение не найдено, выводим сообщение об ошибке
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение не найдено или принадлежит другому студенту");
    }

    @ApiOperation("Список полученных достижений авторизованным студентом, созданных авторизованным или другим студентом - для студента")
    @GetMapping("/student/achievementsCreatedReceived")
    public List<AchievementsReceivedView> getCreatedReceived(@RequestParam
                                                                 @ApiParam(value = "Id студента, чей профиль мы просматриваем. Не передается, если просматривается профиль авторизированного студента. Если чужой профиль, то Not null. >0", example = "4")
                                                                         Optional<Integer> creatorId) {

        //Получаем id авторизованного студента
        int studentId = studentService.getStudentId();
        //Id пользователя создателя достижения
        int userId;
        //Если id студента было передано, то ищем по нему студента, для которого получаем id привязанного к нему пользователя
        if (creatorId.isPresent())
        {
            //Провряем есть ли в базе студент с указанным id
            Student student = studentService.getStudentById(creatorId.get(), Student.class);
            if (student==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент с указанным id не найден");
            else
                userId = student.getUserForStudent().getIdUser();
        }
        //Если id студента не передано, то получаем id авторизованного пользователя
        else
            userId = userService.getUserId();
        //Возвращаем список достижений, полученных авторизованным студентом, а созданных пользователем с полученным userId
        return achieveService.getReceivedAchieveCreatedStudent(getStatusIds(), studentId, userId, AchievementsReceivedView.class);
    }

    @ApiOperation("Список неполученных достижений авторизованным студентом, созданных авторизованным или другим студентом - для студента")
    @GetMapping("/student/achievementsCreatedUnreceived")
    public List<AchievementsView> getCreatedUnreceived(@RequestParam
                                                                 @ApiParam(value = "Id студента, чей профиль мы просматриваем. Не передается, если просматривается профиль авторизированного студента. Если чужой профиль, то Not null. >0", example = "39")
                                                                         Optional<Integer> creatorId) {
        //Список полученных достижений авторизированным студентом
        List<AchievementOfStudent> receivedAchieves;
        //Список неполученных достижений авторизированным студентом
        List<AchievementsView> unreceivedAchieves = new ArrayList<>();
        //Список id достижений системы для полученных достижений авторизированным студентом
        List<Integer> achievesIds = new ArrayList<>();

        //Получаем id авторизованного студента
        int studentId = studentService.getStudentId();
        //Id пользователя создателя достижения
        int userId;

        //Если id студента было передано, то ищем по нему студента, для которого получаем id привязанного к нему пользователя
        if (creatorId.isPresent())
        {
            //Провряем есть ли в базе студент с указанным id
            Student studentTest = studentService.getStudentById(creatorId.get(), Student.class);
            if (studentTest==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент с указанным id не найден");
            userId = studentService.getStudentById(creatorId.get(), Student.class).getUserForStudent().getIdUser();
        }
        //Если id студента не передано, то получаем id авторизованного пользователя
        else
            userId = userService.getUserId();

        //Получаем список полученных авторизированным студентом достижений из созданных пользователем с userId
        receivedAchieves = achieveService.getReceivedAchieveCreatedStudent(getStatusIds(), studentId, userId, AchievementOfStudent.class);

        //Записываем в лист id достижений системы, соответствующих полученным достижениям
        for(AchievementOfStudent achieveReceived : receivedAchieves)
            achievesIds.add(achieveReceived.getAchievementForAchieveOfStudent().getIdAchieve());

        //Если у студента нет полученных достижений, то просто возвращаем список созданных достижений пользователем с userId
        if (achievesIds.size() == 0)
            unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedAndCreatedStudent(getStatusIds(), userId, AchievementsView.class));
        //Если у студента есть полученные достижения из списка созданных пользователем с userId, исключаем эти достижения
        else
            unreceivedAchieves.addAll(achieveService.getAchievementsUnreceivedAndCreatedStudentWithoutReceived(getStatusIds(), achievesIds, userId, AchievementsView.class));

        //Возвращаем список список неполученных авторизованным студентом достижений из созданных пользователей с userId
        return unreceivedAchieves;
    }

    @ApiOperation("Список полученных достижений авторизованным пользователем из полученных достижений другого студента - для студента")
    @GetMapping("/student/achievementsAnotherReceived/{anotherStudentId}")
    public List<AchievementsReceivedView> getAnotherReceived(@PathVariable
                                                                 @ApiParam(value = "Id студента, чей профиль мы просматриваем. Not null. >0", example = "9")
                                                                         Integer anotherStudentId) {
        //Провряем есть ли в базе студент с указанным id
        Student studentTest = studentService.getStudentById(anotherStudentId, Student.class);
        if (studentTest==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент с указанным id не найден");

        //Получаем id текущего пользователя
        int studentId = studentService.getStudentId();

        //Список id полученных достижений авторизованным студентом
        List<Integer> receivedAchievesIds = new ArrayList<>();

        //Получаем список полученных достижений для студента с переданным id
        List<AchievementsReceivedView> receivedAchievesAnotherStudent = achieveService.getReceivedAchievesForStudent(anotherStudentId, AchievementsReceivedView.class);

        //Если у студента с переданным id нет полученных достижений, сразу возвращаем null
        if (receivedAchievesAnotherStudent.size()==0)
            return null;

        //Получаем список id полученных достижений студентом с переданным id
        for(AchievementsReceivedView achieveReceived : receivedAchievesAnotherStudent)
            receivedAchievesIds.add(achieveReceived.getId());
        //Возвращаем список достижений полученных авторизованным студентом, которые входят в список полученых достижений студентом с переданным id
        return achieveService.getReceivedAchievesFromAchievesAnotherStudent(studentId, receivedAchievesIds, AchievementsReceivedView.class);
    }

    @ApiOperation("Список неполученных достижений авторизованным пользователем из полученных достижений другого студента - для студента")
    @GetMapping("/student/achievementsAnotherUnreceived/{anotherStudentId}")
    public List<AchievementsView> getAnotherUnreceived(@PathVariable
                                                           @ApiParam(value = "Id студента, чей профиль мы просматриваем. Not null. >0", example = "9")
                                                                   Integer anotherStudentId) {
        //Провряем есть ли в базе студент с указанным id
        Student studentTest = studentService.getStudentById(anotherStudentId, Student.class);
        if (studentTest==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент с указанным id не найден");

        //Получаем список полученных достижений студентом с переданным id
        List<AchievementOfStudent> receivedAchievesAnotherStudent = achieveService.getReceivedAchievesForStudent(anotherStudentId, AchievementOfStudent.class);

        //Если студент не получал достижения, сразу возвращаем null
        if (receivedAchievesAnotherStudent.size()==0)
            return null;

        //Список полученных авторизованным студентом достижений
        List<AchievementOfStudent> receivedAchieves;
        //Список id полученых достижений студентом с переданным id
        List<Integer> receivedAchievesAnotherStudentIds = new ArrayList<>();
        //Список id достижений системы, соответствующих полученным достижениям авторизованного достижения
        List<Integer> achievesIds = new ArrayList<>();
        List<Integer> achievesAnotherStudentIds = new ArrayList<>();

        //Получаем список id полученных достижений и достижений из списка полученных достижений студента, которого мы просматриваем
        for(AchievementOfStudent achievesAnotherStudent : receivedAchievesAnotherStudent)
        {
            receivedAchievesAnotherStudentIds.add(achievesAnotherStudent.getIdAchieveOfStudent());
            achievesAnotherStudentIds.add(achievesAnotherStudent.getAchievementForAchieveOfStudent().getIdAchieve());
        }

        //Получаем id авторизованного студента и список полученных им достижений из списка полученных достижений студента, которого мы просматриваем
        int studentId = studentService.getStudentId();
        receivedAchieves = achieveService.getReceivedAchievesFromAchievesAnotherStudent(studentId, receivedAchievesAnotherStudentIds, AchievementOfStudent.class);

        for(AchievementOfStudent achieveReceived : receivedAchieves)
            achievesIds.add(achieveReceived.getAchievementForAchieveOfStudent().getIdAchieve());

        List<AchievementsView> unreceivedAchievesFromAchievesAnotherStudent;
        if (achievesIds.size()==0)
            unreceivedAchievesFromAchievesAnotherStudent = achieveService.getUnreceivedAchievesFromAchievesAnotherStudentWithNullReceivedAchieves(achievesAnotherStudentIds, AchievementsView.class);
        else
            unreceivedAchievesFromAchievesAnotherStudent = achieveService.getUnreceivedAchievesFromAchievesAnotherStudent(achievesIds, achievesAnotherStudentIds, AchievementsView.class);
        return unreceivedAchievesFromAchievesAnotherStudent;
    }

    @ApiOperation("Создание заявки на добавление нового достижения в систему - для студента")
    @PostMapping("/student/newAchieve")
    public SuccessfullResponse newAchieve (@RequestBody
                                          @ApiParam(value = "Запрос с данными для создания заявки на добавление нового достижения в систему")
                                                  CreationAchieveRequest creationAchieveRequest) {
        LocalDate todayDate = java.time.LocalDate.now();
        Achievement achievement = new Achievement();
        Achievement testAchievement = achieveService.getAchieveByName(creationAchieveRequest.getAchieveName());
        if (testAchievement != null)
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "Достижение с таким названием уже существует");

        if (creationAchieveRequest.getAchieveStartDate().isBefore(todayDate))
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "Дата начала не может быть раньше текущей даты");

        if (creationAchieveRequest.getAchieveEndDate().isPresent())
        {
            if (creationAchieveRequest.getAchieveEndDate().get().isBefore(creationAchieveRequest.getAchieveStartDate()) || creationAchieveRequest.getAchieveEndDate().get().isEqual(creationAchieveRequest.getAchieveStartDate()))
                throw  new ResponseStatusException(HttpStatus.CONFLICT, "Дата окончания не может быть раньше даты начала или равняться ей");
            else
                achievement.setEndDateAchieve(creationAchieveRequest.getAchieveEndDate().get());
        }
        else
            achievement.setEndDateAchieve(null);

        achievement.setNameAchieve(creationAchieveRequest.getAchieveName());
        achievement.setDescriptionAchieve(creationAchieveRequest.getAchieveDescription());
        achievement.setScoreAchieve(creationAchieveRequest.getAchieveScore());
        achievement.setStartDateAchieve(creationAchieveRequest.getAchieveStartDate());

        File file = new File();
        file.setDataFile(creationAchieveRequest.getPhoto());
        file.setFormatFile(creationAchieveRequest.getFormat().toLowerCase());
        fileService.saveFile(file);

        achievement.setFileAchieve(file);

        achieveService.createNewAchievement(achievement, userService.getUserId(), creationAchieveRequest.getCategoryId(), creationAchieveRequest.getRewardId());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Заявка на создание достижения успешно отправлена");
    }

    @ApiOperation("Создание нового достижения - для админа")
    @PostMapping("/admin/newAchieve")
    public SuccessfullResponse newAchieveForAdmin (@RequestBody
                                      @ApiParam(value = "Запрос с данными для добавления нового достижения в систему")
                                                      CreationAchieveForAdminRequest creationAchieveForAdminRequest) {
        Achievement achievement = new Achievement();
        Achievement testAchievement = achieveService.getAchieveByName(creationAchieveForAdminRequest.getAchieveName());
        if (testAchievement != null) {
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "Достижение с таким названием уже существует");
        }

        if (creationAchieveForAdminRequest.getAchieveEndDate().isPresent())
        {
            if (creationAchieveForAdminRequest.getAchieveEndDate().get().compareTo(creationAchieveForAdminRequest.getAchieveStartDate())!=1)
                throw  new ResponseStatusException(HttpStatus.CONFLICT, "Дата окончания не может быть раньше даты начала или равняться ей");
            else
                achievement.setEndDateAchieve(creationAchieveForAdminRequest.getAchieveEndDate().get());
        }
        else
            achievement.setEndDateAchieve(null);

        achievement.setNameAchieve(creationAchieveForAdminRequest.getAchieveName());
        achievement.setDescriptionAchieve(creationAchieveForAdminRequest.getAchieveDescription());
        achievement.setScoreAchieve(creationAchieveForAdminRequest.getAchieveScore());
        achievement.setStartDateAchieve(creationAchieveForAdminRequest.getAchieveStartDate());

        File file = new File();
        file.setDataFile(creationAchieveForAdminRequest.getPhoto());
        file.setFormatFile(creationAchieveForAdminRequest.getFormat().toLowerCase());
        fileService.saveFile(file);

        achievement.setFileAchieve(file);

        achieveService.createNewAchievementForAdmin(achievement, userService.getUserId(), creationAchieveForAdminRequest.getCategoryId(), creationAchieveForAdminRequest.getRewardId(), creationAchieveForAdminRequest.getStatusActiveId());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Достижение успешно создано");
    }

    @ApiOperation("Список заявок на создание достижения авторизованным или другим студентом - для студента")
    @GetMapping("/student/achievementsCreated")
    public List<AchievementsCreatedView> getCreated(
            @RequestParam
            @ApiParam(value = "Id студента. Может не передаваться. Если не передается - берется id текущего авторизованного студента", example = "7")
                    Optional<Integer> studentId
    ) {
        int creatorId;
        Student tempStudent;
        if (studentId.isEmpty()) {
            creatorId = userService.getUserId();
        } else {
           tempStudent = studentService.getStudentById(studentId.get(), Student.class);
           creatorId = tempStudent.getUserForStudent().getIdUser();
        }
        return achieveService.getAchieveByCreator(creatorId, AchievementsCreatedView.class);
    }

    @ApiOperation("Просмотр конкретной заявки на создание достижения - для студента")
    @GetMapping("/student/achievementsCreated/{achieveId}")
    public AchievementCreatedView getCreatedAchieve(@PathVariable
                                                 @ApiParam (value = "Id заявки на создание достижения. Not null, >0", example = "13")
                                                         int achieveId) {
        int creatorId = userService.getUserId();
        Achievement achievement = achieveService.getAchieveById(achieveId, Achievement.class);
        if (achievement==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение с указанным id не найдено");
        return achieveService.getCreatedAchieveOfStudent(creatorId, achieveId, AchievementCreatedView.class);
    }




    //////////////////////////////////////////////////////////////////////////////////
    /*                                   Админ                                      */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список всех статусов активности достижения - для админа")
    @GetMapping("/admin/getStatusAchieve")
    public List<StatusActiveView> getStatusAchieve() {
        return achieveService.getAllStatus(StatusActiveView.class);
    }

    @ApiOperation("Изменение данных достижения")
    @PutMapping("/admin/changeAchieve/{achieveId}")
    public SuccessfullResponse changeDataOfAchieve(
            @PathVariable
            @ApiParam (value = "Id достижения. Not null. >0", example = "19", required = true)
                    int achieveId,
            @RequestBody EditAchieveForAdminRequest editAchieveForAdminRequest) throws MessagingException {

        Achievement achievement = achieveService.getAchieveById(achieveId, Achievement.class);
        User findUser = achievement.getCreatorOfAchieve();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение данных достижения " + achievement.getNameAchieve());
        String messageText = "";

        //Проверяем, есть ли у нас достижения с переданным id
        if (achievement==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение с указанным id не найдено");

        if (editAchieveForAdminRequest.getAchieveName().isPresent()) {
            if (!editAchieveForAdminRequest.getAchieveName().get().equals("") && !editAchieveForAdminRequest.getAchieveName().get().equals(achievement.getNameAchieve()))
                achievement.setNameAchieve(Character.toUpperCase(editAchieveForAdminRequest.getAchieveName().get().charAt(0))+editAchieveForAdminRequest.getAchieveName().get().substring(1).toLowerCase());
            messageText += "Название достижения изменено на: " + editAchieveForAdminRequest.getAchieveName() + "\n";
        }

        if (editAchieveForAdminRequest.getAchieveDescription().isPresent()) {
            if (!editAchieveForAdminRequest.getAchieveDescription().get().equals("") && !editAchieveForAdminRequest.getAchieveDescription().get().equals(achievement.getDescriptionAchieve()))
                achievement.setDescriptionAchieve(Character.toUpperCase(editAchieveForAdminRequest.getAchieveDescription().get().charAt(0))+editAchieveForAdminRequest.getAchieveDescription().get().substring(1).toLowerCase());
            messageText += "Описание достижения изменено на: " + editAchieveForAdminRequest.getAchieveDescription() + "\n";
        }

        if (editAchieveForAdminRequest.getAchieveScore().isPresent()) {
            if (editAchieveForAdminRequest.getAchieveScore().get()!=0 && achievement.getScoreAchieve()!=editAchieveForAdminRequest.getAchieveScore().get())
                achievement.setScoreAchieve(editAchieveForAdminRequest.getAchieveScore().get());
            messageText += "Количество очков за достижение изменено на: " + editAchieveForAdminRequest.getAchieveScore() + "\n";
        }

        if (editAchieveForAdminRequest.getAchieveStartDate().isPresent()) {
            LocalDate nowDate = LocalDate.now();
            if (editAchieveForAdminRequest.getAchieveStartDate().get().isAfter(nowDate) && editAchieveForAdminRequest.getAchieveStartDate().get().compareTo(achievement.getStartDateAchieve())!=0 ) {
                achievement.setStartDateAchieve(editAchieveForAdminRequest.getAchieveStartDate().get());
                messageText += "Дата начала выполнения достижения изменена на: " + editAchieveForAdminRequest.getAchieveStartDate() + "\n";
            }
            else
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата начала не должна быть раньше завтрашнего дня");
        }

        if (editAchieveForAdminRequest.getAchieveEndDate().isPresent() ) {
            if (editAchieveForAdminRequest.getAchieveEndDate().get().isAfter(achievement.getStartDateAchieve())) {
                achievement.setEndDateAchieve(editAchieveForAdminRequest.getAchieveEndDate().get());
                messageText += "Дата окончания выполнения достижения изменена на: " + editAchieveForAdminRequest.getAchieveEndDate() + "\n";
            }
            else
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата окончания не может быть раньше даты начала или равняться ей");
        }

        if (editAchieveForAdminRequest.getRewardId().isPresent())
            if (editAchieveForAdminRequest.getRewardId().get()!=0) {
                achievement.setRewardOfAchieve(rewardService.getReward(editAchieveForAdminRequest.getRewardId().get()));
                Reward reward = rewardService.getReward(editAchieveForAdminRequest.getRewardId().get());
                messageText += "Награда за выполнения достижения изменена на: " + reward.getNameReward() + "\n";
            }

        if (editAchieveForAdminRequest.getCategoryId().isPresent())
            if (editAchieveForAdminRequest.getCategoryId().get()!=0) {
                achievement.setCategoryOfAchieve(achieveService.getCategory(editAchieveForAdminRequest.getCategoryId().get()));
                Category category = achieveService.getCategory(editAchieveForAdminRequest.getCategoryId().get());
                messageText += "Категория достижения изменена на: " + category.getNameCategory() + "\n";
            }

        if (editAchieveForAdminRequest.getPhoto().isPresent())
            if (editAchieveForAdminRequest.getPhoto().get().length<1) {
                File file = achievement.getFileAchieve();
                file.setDataFile(editAchieveForAdminRequest.getPhoto().get());
                fileService.resetFile(file);
            }

        if (editAchieveForAdminRequest.getFormat().isPresent()) {
            if (!editAchieveForAdminRequest.getFormat().get().equals("") && !editAchieveForAdminRequest.getFormat().get().equals(achievement.getFileAchieve().getFormatFile())) {
                File file = achievement.getFileAchieve();
                file.setFormatFile(editAchieveForAdminRequest.getFormat().get().toLowerCase());
                messageText += "Тип файла изображения достижения изменен на: " + editAchieveForAdminRequest.getFormat() + "\n";
                fileService.resetFile(file);
            }
        }

        if (editAchieveForAdminRequest.getStatusActiveId().isPresent()) {
            if (editAchieveForAdminRequest.getStatusActiveId().get()!=0) {
                achievement.setStatusActiveOfAchieve(achieveService.getStatusActive(editAchieveForAdminRequest.getStatusActiveId().get()));
            }
        }
        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Данные достижения успешно изменены");
    }


    //////////////////////////////////////////////////////////////////////////////////
    /*                                Модератор                                     */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Список части статусов активности достижения, доступных модератору")
    @GetMapping("/moderator/getStatusAchieve")
    public List<StatusActiveView> getStatusAchieveAdmin() {
        List<StatusActiveView> statusAchieve = new ArrayList<>();
        statusAchieve.add(achieveService.getStatusActiveById(5, StatusActiveView.class));
        statusAchieve.add(achieveService.getStatusActiveById(6, StatusActiveView.class));
        return statusAchieve;
    }


    //////////////////////////////////////////////////////////////////////////////////
    /*                          Модератор      Админ                                */
    //////////////////////////////////////////////////////////////////////////////////

    @ApiOperation("Изменения статуса активности у достижения/заявки - для админа/модера")
    @PutMapping("/upr/changeStatusAchieve/{achieveId}/{statusId}")
    public SuccessfullResponse changeStatusAchieve(@PathVariable
                                                  @ApiParam(value = "Id статуса активности достижения. Not null. [1,6] для админа. [5,6] для модератора", example = "2")
                                                          int statusId,
                                              @PathVariable
                                                  @ApiParam(value = "Id достижения/заявки. Not null. >0", example = "13")
                                                          int achieveId) throws MessagingException {
        Achievement achievement = achieveService.getAchieveById(achieveId, Achievement.class);
        User findUser = achievement.getCreatorOfAchieve();
        String userEmail = findUser.getEmailUser();
        MimeMessage notificationMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(notificationMessage, true);
        helper.setTo(userEmail);
        helper.setSubject("Изменение статуса активности достижения " + achievement.getNameAchieve());
        String messageText = "";

        if (achievement!=null)
        {
            int roleId = userService.getUser().getRoleUser().getIdRole();
            if (roleId==2 && (statusId<1 || statusId>6))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус не найден");
            if (roleId==3 && (statusId<5|| statusId>6))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус не найден");

            String oldData = achievement.getStatusActiveOfAchieve().getStatusActive();
            achievement.setStatusActiveOfAchieve(achieveService.getStatusActive(statusId));
            achievement = achieveService.saveAchievement(achievement);
            String newData = achievement.getStatusActiveOfAchieve().getStatusActive();
            messageText += "Статус активности Вашего достижения изменен на " + achieveService.getStatusActive(statusId).getStatusActive();
            logService.createNewLog(userService.getUserId(), 1, achievement.getIdAchieve(), oldData, newData);
        }
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение не найдено");
        helper.setText(messageText);
        mailSender.send(notificationMessage);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Статус достижения успешно изменен");
    }

    @ApiOperation("Список достижений в системе - для админа/модера")
    @GetMapping("/upr/achievements")
    public List<AllAchievementsForAdminView> getAchieve(@RequestParam
                                                      @ApiParam(value = "Id категории. Фильтр, может не передаваться, тогда выводятся достижения всех категорий. Если передается - Not null. [1,5]", example = "4")
                                                              Optional<Integer> categoryId,
                                                        @RequestParam
                                                      @ApiParam(value = "Id статуса активности достижения. Фильтр, может не передаваться, тогда выводятся достижения всех категорий. Если передается - Not null. [1,6] для админа. [5,6] для модератора", example = "3")
                                                          Optional<Integer> statusId) {
        List<AllAchievementsForAdminView> achieves;
        if (statusId.isEmpty()) {
            if(categoryId.isEmpty())
                achieves = achieveService.getAllAchieve(AllAchievementsForAdminView.class);
            else
                achieves = achieveService.getCategoryOfAchieve(categoryId.get(), AllAchievementsForAdminView.class);
        }
        else {
            int roleId = userService.getUser().getRoleUser().getIdRole();
            if (roleId==2 && (statusId.get()<1 || statusId.get()>6))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус не найден");
            if (roleId==3 && (statusId.get()<1|| statusId.get()>6))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статус не найден");
            if(categoryId.isEmpty())
                achieves = achieveService.getAchieveStatus(statusId.get(), AllAchievementsForAdminView.class);
            else
            {
                if (categoryId.get()<1 || categoryId.get()>5)
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена");
                achieves = achieveService.getAchieveStatusCategory(categoryId.get(), statusId.get(), AllAchievementsForAdminView.class);
            }
        }

        return achieves;
    }

    @ApiOperation("Конкретное достижение в системе - для админа/модера")
    @GetMapping("/upr/achievements/{achieveId}")
    public AchievementAdminView getAchieveByIdForAdmin(@PathVariable
                                                   @ApiParam(value = "Id достижения. Not null, >0", example = "27")
                                                           int achieveId) {
        AchievementAdminView achievementAdmin = achieveService.getAchieveById(achieveId, AchievementAdminView.class);

        if(achievementAdmin!=null)
            return achievementAdmin;
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Достижение не найдено");
    }
}
