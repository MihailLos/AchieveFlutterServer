package com.example.demo.controller;

import com.example.demo.config.jwt.JwtProvider;
import com.example.demo.controller.request.*;
import com.example.demo.controller.response.*;
import com.example.demo.entity.*;
import com.example.demo.entity.File;
import com.example.demo.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasText;

import java.net.HttpURLConnection;
import org.json.*;

@Api(description = "Контроллер авторизации, регистрации")
@RestController
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private FileService fileService;
    @Autowired
    private BanService banService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private LogService logService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public static final String REFRESH = "Refresh";

    @ApiOperation("Авторизация пользователей, кроме студентов, авторизующихся через систему защиты ЭИОС")
    @PostMapping("/auth")
    public AuthResponse authorizeUser(@RequestBody
                                          @ApiParam(value = "Запрос с данными для авторизации пользователя")
                                                  AuthRequest authRequest,
                                      @RequestParam
                                      @ApiParam(value = "Название роли авторизуемого пользователя. Если null, то роль Студент", example = "Модератор", allowableValues = "Модератор, Администратор")
                                              Optional<String> filterName) {
        //Проверяем зарегистрирован ли пользователь с такой почтой и правильно ли введен пароль
        if (filterName.isEmpty())
            filterName = Optional.of("Студент");
        User user = userService.findByEmailAndPasswordAndRole(authRequest.getEmail(), authRequest.getPassword(), filterName.get());

        //Проверяем удален ли пользователь
        if (user.getStatusUser() == userService.getStatusUser("Удален"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь удален");

        //Проверяем удален ли пользователь без права восстановления
        if (user.getStatusUser() == userService.getStatusUser("Аннигилирован"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь удален без права восстановления");

        //Проверяем забанен ли пользователь
        if (user.getStatusUser() == userService.getStatusUser("Забанен")) {
            Ban ban = userService.getBan(user.getIdUser());
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dateNow = new Date();
            String stringDate = simpleDateFormat.format(dateNow);
            //Если дата окончания баны приходится на текущую дату, то бан удаляется, статус пользователя меняетяс на "Активен" и авторизация проходит
            if (ban.getDateEndBan().toString().equals(stringDate)) {
                user.setStatusUser(userService.getStatusUser("Активен"));
                userService.saveUser(user);
                banService.deleteBan(ban);
            }
            //Если бан еще не кончился, выводится соответствующее сообщение
            else
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь забанен по причине: " + ban.getReasonBan() + ". Бан истечет " + ban.getDateEndBan());
        }

        //Если все проверки прошли успешно, генерируем токены и возвращаем на клиент
        String accessToken = jwtProvider.generateAccessToken(user.getIdUser());
        String refreshToken = jwtProvider.generateRefreshToken(user.getIdUser());
        return new AuthResponse(accessToken, refreshToken);
    }

    @ApiOperation("Авторизация студента через систему защиты ЭИОС")
    @PostMapping("/authEios")
    public AuthEIOSForStudentResponse authorizeEiosStudent(@RequestBody
                                      @ApiParam(value = "Запрос с данными для авторизации студента через систему защиты ЭИОС")
                                                                   AuthEIOSStudentRequest authEIOSStudentRequest) {

        String accessToken = ""; // Токен доступа от ИС учета достижений студентов
        String refreshToken = ""; // Рефреш токен от ИС учета достижений студентов
        String accessTokenEios; // Токен доступа от системы защиты ЭИОС
        String userLastName; // Фамилия пользователя
        String userFirstName; // Имя пользователя
        String groupName; // Название группы студента, привязанного к пользователю
        String facultyName; // Название института студента
        String specialityName; // Название специальности студента
        String qualificationName; // Название степени обучения студента
        String userType; // Тип пользователя
        Integer course; // Курс обучающегося

        HttpURLConnection connectionEiosAuth; // Подключение к системе защиты ЭИОС
        String enteredLogin = authEIOSStudentRequest.getLogin(); // Логин из запроса
        String enteredPassword = authEIOSStudentRequest.getPassword(); // Пароль из запроса
        StringBuilder responseEiosAuth = null; // Ответ с сервера системы защиты ЭИОС

        try {
            // Создаем подключение к системе защиты ЭИОС
            URL url = new URL("https://api-next.kemsu.ru/api/auth");
            connectionEiosAuth = (HttpURLConnection) url.openConnection();
            connectionEiosAuth.setRequestMethod("POST");
            connectionEiosAuth.setRequestProperty("Content-Type", "application/json");
            connectionEiosAuth.setRequestProperty("Accept", "application/json");
            connectionEiosAuth.setDoOutput(true);

            String jsonInputString = "{\"login\":\"" + enteredLogin + "\", \"password\":\"" + enteredPassword + "\"}";

            //Отправляем запрос на авторизацию через ЭИОС
            DataOutputStream outputStreamEiosAuth = new DataOutputStream (connectionEiosAuth.getOutputStream());
            outputStreamEiosAuth.writeBytes(jsonInputString);
            outputStreamEiosAuth.close();

            //Получаем ответ в формате JSON и преобразуем его в строку
            InputStream inputStreamEiosAuth = connectionEiosAuth.getInputStream();
            BufferedReader bufferedReaderEiosAuth = new BufferedReader(new InputStreamReader(inputStreamEiosAuth));
            responseEiosAuth = new StringBuilder();
            String line;
            while ((line = bufferedReaderEiosAuth.readLine()) != null) {
                responseEiosAuth.append(line);
                responseEiosAuth.append('\r');
            }
            bufferedReaderEiosAuth.close();
        } catch (IOException e) {
            System.out.println("Не удалось подключиться к системе защиты ЭИОС");
            e.printStackTrace();
        }

        // Преобразуем полученный ответ в строку
        String jsonEiosAuthString = convertUTF8(responseEiosAuth.toString());

        // Преобразовываем строку в JSON
        JSONObject jsonEiosAuth = new JSONObject(jsonEiosAuthString);

        //Получаем инфу из JSON системы защиты ЭИОС
        userType = jsonEiosAuth.getJSONObject("userInfo").getString("userType");
        userFirstName = convertUTF8(jsonEiosAuth.getJSONObject("userInfo").getString("firstName"));
        userLastName = convertUTF8(jsonEiosAuth.getJSONObject("userInfo").getString("lastName"));
        String userLogin = convertUTF8(jsonEiosAuth.getJSONObject("userInfo").getString("login"));
        int userId = jsonEiosAuth.getJSONObject("userInfo").getInt("id");
        String userEmail = convertUTF8(jsonEiosAuth.getJSONObject("userInfo").getString("email"));
        accessTokenEios = convertUTF8(jsonEiosAuth.getString("accessToken"));

        HttpURLConnection conDekanat; // Подключение к ИС Деканат
        StringBuilder responseDekanat = null; // Ответ с сервера ИС Деканат

        try {
            // Создаем подключение к ИС "Деканат" для получения учебной карты студента
            String baseURL = "https://api.kemsu.ru/api/dekanat/students/study-cards";
            baseURL += "?accessToken=" + accessTokenEios;
            URL urlDekanat = new URL(baseURL);
            conDekanat = (HttpURLConnection) urlDekanat.openConnection();
            conDekanat.setRequestMethod("GET");

            // Принимаем ответ с сервера и записываем в строку
            InputStream inputStreamDekanat = conDekanat.getInputStream();
            BufferedReader bufferedReaderDekanat = new BufferedReader(new InputStreamReader(inputStreamDekanat));
            responseDekanat = new StringBuilder();
            String line1;
            while ((line1 = bufferedReaderDekanat.readLine()) != null) {
                responseDekanat.append(line1);
                responseDekanat.append('\r');
            }
            bufferedReaderDekanat.close();
        } catch (IOException e) {
            System.out.println("Не удалось получить учебную карту студента!");
            e.printStackTrace();
        }

        // Преобразуем полученный ответ в строку
        String jsonDekanatString = convertUTF8(responseDekanat.toString());
        jsonDekanatString = jsonDekanatString.substring(1, jsonDekanatString.length()-1);

        // Преобразовываем строку в JSON
        JSONObject jsonDekanat = new JSONObject(jsonDekanatString);

        int studId = jsonDekanat.getInt("ID");
        int groupId = jsonDekanat.getInt("GROUP_ID");
        int facultyId = jsonDekanat.getInt("FACULTY_ID");
        int specialityId = jsonDekanat.getInt("SPECIALITY_ID");
        groupName = jsonDekanat.isNull("GROUP_NAME") ? "Группа не задана" : convertUTF8(jsonDekanat.getString("GROUP_NAME"));
        facultyName = jsonDekanat.isNull("FACULTY") ? "Институт не задан" : convertUTF8(jsonDekanat.getString("FACULTY"));
        specialityName = jsonDekanat.isNull("SPECIALITY") ? "Специальность не задана" : convertUTF8(jsonDekanat.getString("SPECIALITY"));
        qualificationName = jsonDekanat.isNull("QUALIFICATION") ? "Квалификация не задана" : convertUTF8(jsonDekanat.getString("QUALIFICATION"));
        course = jsonDekanat.isNull("COURS") ? null : jsonDekanat.getInt("COURS");

        // Занесение учебных данных обучающегося в базу, перепроверка данных, если они уже были занесены
        Institute institute = educationService.getInstitute(facultyId);
        if (institute != null) {
            if (!Objects.equals(institute.getInstituteName(), facultyName))
                institute.setInstituteName(facultyName);
            if (!Objects.equals(institute.getInstituteFullName(), facultyName))
                institute.setInstituteFullName(facultyName);
            institute = educationService.createNewInstitute(institute);
        } else {
            institute = new Institute();
            institute.setIdInstitute(facultyId);
            institute.setInstituteName(facultyName);
            institute.setInstituteFullName(facultyName);
            institute = educationService.createNewInstitute(institute);
        }

        Stream stream = educationService.getStream(specialityId);
        if (stream != null) {
            if (!Objects.equals(stream.getStreamFullName(), specialityName))
                stream.setStreamName(specialityName);
            if (!Objects.equals(stream.getInstituteForStream(), institute))
                stream.setInstituteForStream(institute);
            stream = educationService.createNewStream(stream);
        } else {
            stream = new Stream();
            stream.setIdStream(specialityId);
            stream.setStreamName(specialityName);
            stream.setStreamFullName(specialityName);
            stream.setInstituteForStream(institute);
            stream = educationService.createNewStream(stream);
        }

        Group group = educationService.getGroup(groupId);
        if (group != null) {
            if (!Objects.equals(group.getGroupName(), groupName))
                group.setGroupName(groupName);
            if (!Objects.equals(group.getStreamForGroup(), stream))
                group.setStreamForGroup(stream);
            group = educationService.createNewGroup(group);
        } else {
            group = new Group();
            group.setIdGroup(groupId);
            group.setGroupName(groupName);
            group.setStreamForGroup(stream);
            group = educationService.createNewGroup(group);
        }

        FormOfEducation formOfEducation = educationService.getFormEducationByName(qualificationName);
        if (formOfEducation != null) {
            if (!Objects.equals(formOfEducation.getFormEducationName(), qualificationName))
                formOfEducation.setFormEducationName(qualificationName);
            formOfEducation = educationService.createNewFormOfEducation(formOfEducation);
        } else {
            formOfEducation = new FormOfEducation();
            formOfEducation.setFormEducationName(qualificationName);
            formOfEducation = educationService.createNewFormOfEducation(formOfEducation);
        }

        //Проверяем, что вошедший пользователь - обучающийся
        if (!isUserStudent(userType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь не является обучающимся");
        }

        User user = userService.findUserByEiosLogin(authEIOSStudentRequest.getLogin());

        // Если пользователь зарегистрирован в системе
        if (user != null) {
            // Проверка на изменение личных данных
            if (!passwordEncoder.matches(authEIOSStudentRequest.getPassword(),user.getPasswordUser()))
                user.setPasswordUser(authEIOSStudentRequest.getPassword());
            if (!Objects.equals(user.getFirstNameUser(), userFirstName))
                user.setFirstNameUser(userFirstName);
            if (!Objects.equals(user.getLastNameUser(), userLastName))
                user.setLastNameUser(userLastName);
            if (!Objects.equals(user.getEiosLogin(), userLogin))
                user.setEiosLogin(userLogin);
            if (!Objects.equals(user.getEmailUser(), userEmail))
                user.setEmailUser(userEmail);
            user = userService.createNewUserForStudent(user);

            //Проверяем удален ли пользователь
            if (user.getStatusUser() == userService.getStatusUser("Удален"))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь удален");
            //Проверяем удален ли пользователь без права восстановления
            if (user.getStatusUser() == userService.getStatusUser("Аннигилирован"))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь удален без права восстановления");

            //Проверяем забанен ли пользователь
            if (user.getStatusUser() == userService.getStatusUser("Забанен")) {
                Ban ban = userService.getBan(user.getIdUser());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date dateNow = new Date();
                String stringDate = simpleDateFormat.format(dateNow);
                //Если дата окончания баны приходится на текущую дату, то бан удаляется, статус пользователя меняетяс на "Активен" и авторизация проходит
                if (ban.getDateEndBan().toString().equals(stringDate)) {
                    user.setStatusUser(userService.getStatusUser("Активен"));
                    userService.saveUser(user);
                    banService.deleteBan(ban);
                }
                //Если бан еще не кончился, выводится соответствующее сообщение
                else
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь забанен по причине: " + ban.getReasonBan() + ". Бан истечет " + ban.getDateEndBan());
            }

            Student student = studentService.getStudent(user);
            if (!Objects.equals(student.getStreamStudent(), stream))
                student.setStreamStudent(stream);
            if (!Objects.equals(student.getUserForStudent(), user))
                student.setUserForStudent(user);
            if (!Objects.equals(student.getGroupStudent(), group))
                student.setGroupStudent(group);
            if (!Objects.equals(student.getInstituteStudent(), institute))
                student.setInstituteStudent(institute);
            if (!Objects.equals(student.getFormOfEducationStudent(), formOfEducation))
                student.setFormOfEducationStudent(formOfEducation);
            if (!Objects.equals(student.getFormOfEducationStudent(), formOfEducation))
                student.setFormOfEducationStudent(formOfEducation);
            if (!Objects.equals(student.getCourse(), course))
                student.setCourse(course);
            student = studentService.createNewEiosStudent(student);

            //Если все проверки прошли успешно, генерируем токены и возвращаем на клиент
            accessToken = jwtProvider.generateAccessToken(user.getIdUser());
            refreshToken = jwtProvider.generateRefreshToken(user.getIdUser());
        }
        else {
            user = new User();
            //Записываем в нового пользователя введенные данные
            user.setPasswordUser(authEIOSStudentRequest.getPassword());
            user.setEiosLogin(authEIOSStudentRequest.getLogin());;
            user.setFirstNameUser(Character.toUpperCase(userFirstName.charAt(0)) + userFirstName.substring(1).toLowerCase());
            user.setLastNameUser(Character.toUpperCase(userLastName.charAt(0)) + userLastName.substring(1).toLowerCase());
            user.setEiosId(userId);
            user.setEmailUser(userEmail);
            user = userService.createNewUserForStudent(user);

            //В качестве фото профиля берется стандратная иконка
            File studentAvatarFile = fileService.getFileById(23, File.class);
            fileService.resetFile(studentAvatarFile);

            //Записываем в нового студента введенные данные
            Student student = new Student();
            student.setIdStudent(studId);
            student.setUserForStudent(user);
            student.setFileStudent(studentAvatarFile);
            student.setFormOfEducationStudent(formOfEducation);
            student.setInstituteStudent(institute);
            student.setStreamStudent(stream);
            student.setGroupStudent(group);
            student.setCourse(course);
            student = studentService.createNewEiosStudent(student);

            accessToken = jwtProvider.generateAccessToken(user.getIdUser());
            refreshToken = jwtProvider.generateRefreshToken(user.getIdUser());
        }

        return new AuthEIOSForStudentResponse(accessToken, refreshToken);
    }

    // Кодирование строки String в UTF-8
    public String convertUTF8(String string) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(string);
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    public boolean isUserStudent(String userType) {
        return Objects.equals(userType, "обучающийся");
    }

    @ApiOperation("Новый токен доступа, если старый устарел. Для получения нового токена необходимо отправить Рефреш токен в header в виде: ключ=Refresh, значение=Refresh значениеРефрешТокена")
    @GetMapping("/newToken")
    public NewTokenResponse generateNewToken(HttpServletRequest request) {

        //Получаем пару ключ-значение, где ключ=Refresh
        String header = request.getHeader(REFRESH);
        String refreshToken;

        //Если такая пара есть, считываем рефреш токен
        if (hasText(header) && header.startsWith("Refresh "))
            refreshToken = header.substring(8);
            //Если нет, сохраняем пустое значение
        else
            refreshToken = null;

        //Если рефреш токен не пустой
        if (refreshToken != null) {
            //И при этом валидный, то генерируем и отдаем новый токен доступа
            if (jwtProvider.validateRefreshToken(refreshToken)) {
                String subjectRefreshToken = jwtProvider.getSubjectFromRefreshToken(refreshToken);
                int integerSubject = Integer.parseInt(subjectRefreshToken);
                String token = jwtProvider.generateAccessToken(integerSubject);
                return new NewTokenResponse(token);
            }
            //Если рефреш токен не валидный, выводим сообщение об ошибке
            else
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Рефреш токен недействителен");
        }
        //Если рефреш токен пуст, выводим сообщение об ошибке
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Рефреш токен пуст");
    }

    @ApiOperation("Регистрация пользователя в роли администратора. Доступно только админу")
    @PostMapping("/admin/registerAdmin")
    public SuccessfullResponse registerAdmin(@RequestBody
                                            @ApiParam(value = "Запрос с данными для регистрации пользователя в роли администратора")
                                                    RegistrationAdminRequest registrationAdminRequest) {
        //Проверяем правильность введенных данных
        validateInputData(registrationAdminRequest.getEmail(), registrationAdminRequest.getPassword(), registrationAdminRequest.getFirstName(), registrationAdminRequest.getLastName());

        //Проверка на наличие зарегистрированного пользователя с указанной электронной почтой
        User test = userService.findByEmail(registrationAdminRequest.getEmail());
        if (test != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует!");

        //Записываем в нового пользователя введенные данные
        User user = new User();
        user.setPasswordUser(registrationAdminRequest.getPassword());
        user.setEmailUser(registrationAdminRequest.getEmail());
        user.setFirstNameUser(Character.toUpperCase(registrationAdminRequest.getFirstName().charAt(0)) + registrationAdminRequest.getFirstName().substring(1).toLowerCase());
        user.setLastNameUser(Character.toUpperCase(registrationAdminRequest.getLastName().charAt(0)) + registrationAdminRequest.getLastName().substring(1).toLowerCase());
        userService.createNewUserForAdmin(user);

        logService.createNewLog(userService.getUserId(), 6, user.getIdUser(), null, user.getFirstNameUser() + " " + user.getLastNameUser() + " - " + user.getRoleUser());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Регистрация прошла успешно");
    }

    private void validateInputData(String email, String password, String firstName, String lastName) {
        //Проверка на валидность введенной электронной почты
        EmailValidator emailValidator = EmailValidator.getInstance();
        boolean validEmail = emailValidator.isValid(email);
        if (!validEmail)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email введен не корректно");

        //Проверка на валидность введенных фамилии и имени
        Pattern pattern = Pattern.compile("[а-яА-Я]{2,}", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcherFirstName = pattern.matcher(firstName);
        Matcher matcherLastName = pattern.matcher(lastName);
        if (!matcherFirstName.matches() || !matcherLastName.matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для фамилии и имени разрешено использовать только русские буквы. Минимальная длина составляет 2 символа");

        //Проверка на длину пароля
        if (password.length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен состоять не менее чем из 8 символов");

        //Проверка на наличие зарегистрированного пользователя с указанной электронной почтой
        User test = userService.findByEmail(email);
        if (test != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует!");
    }

    @ApiOperation("Получение Id и роли пользователя по Access-токену")
    @GetMapping("/userData")
    public UserDataResponse getUserData() {
        int userId = userService.getUserId();
        User user = userService.getUser();

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь не авторизован");
        }

        return new UserDataResponse(userId, user.getRoleUser().getNameRole(), user.getFirstNameUser(), user.getLastNameUser());
    }

    //Принимает данные в body, необходимые для регистрации. Описание в RegistrationAdminRequest
    //Возвращает сообщение об успехе/неудаче
    @ApiOperation("Регистрация пользователя в роли модератора. Доступно только админу")
    @PostMapping("/admin/registerModer")
    public SuccessfullResponse registerModerNew(
            @RequestBody
            @ApiParam(value = "Запрос с данными для регистрации пользователя в роли модератора")
                    RegistrationModerRequestNew registrationModerRequestNew,
            @RequestParam
            @ApiParam(value = "Список id институтов, к которым будет открыт доступ у модератора. Если null, то институты модератору присваиваться не будут", example = "1,2,3")
                    Optional<List<Integer>> listOfInstitutesId
    ) {
        //Проверяем правильность введенных данных
        validateInputData(registrationModerRequestNew.getEmail(), registrationModerRequestNew.getPassword(), registrationModerRequestNew.getFirstName(), registrationModerRequestNew.getLastName());

        //Записываем в нового пользователя введенные данные
        User user = new User();
        user.setPasswordUser(registrationModerRequestNew.getPassword());
        user.setEmailUser(registrationModerRequestNew.getEmail());
        user.setFirstNameUser(Character.toUpperCase(registrationModerRequestNew.getFirstName().charAt(0)) + registrationModerRequestNew.getFirstName().substring(1).toLowerCase());
        user.setLastNameUser(Character.toUpperCase(registrationModerRequestNew.getLastName().charAt(0)) + registrationModerRequestNew.getLastName().substring(1).toLowerCase());
        userService.createNewUserForModer(user);

        //Создаем нового подератора и записываем в него данные
        Moderator moderator = new Moderator();
        moderator.setUser(user);
        userService.saveModer(moderator);
        Set<Institute> institutePool = new HashSet<>();

        ModeratorInstitute moderatorInstitute = new ModeratorInstitute();
        if (!listOfInstitutesId.isEmpty()) {
            for (Integer instituteId : listOfInstitutesId.get()) {
                institutePool.add(educationService.getInstitute(instituteId));
            }

            moderator.setInstitutes(institutePool);
            userService.saveModer(moderator);
        }

        return new SuccessfullResponse(HttpStatus.OK.value(), "Модератор успешно создан");
    }

    @ApiOperation("Регистрация пользователя в роли модератора. Доступно только админу")
    @PostMapping("/admin/registerDekanat")
    public SuccessfullResponse registerModerNew(
            @RequestBody
            @ApiParam(value = "Запрос с данными для регистрации пользователя в роли сотрудника дирекции")
                    RegistrationDekanatRequest registrationDekanatRequest,
            @RequestParam
            @ApiParam(value = "Id института, которой принадлежит дирекция", example = "1")
                    Integer instituteId
    ) {
        //Проверяем правильность введенных данных
        validateInputData(registrationDekanatRequest.getEmail(), registrationDekanatRequest.getPassword(), registrationDekanatRequest.getFirstName(), registrationDekanatRequest.getLastName());

        //Записываем в нового пользователя введенные данные
        User user = new User();
        user.setPasswordUser(registrationDekanatRequest.getPassword());
        user.setEmailUser(registrationDekanatRequest.getEmail());
        user.setFirstNameUser(Character.toUpperCase(registrationDekanatRequest.getFirstName().charAt(0)) + registrationDekanatRequest.getFirstName().substring(1).toLowerCase());
        user.setLastNameUser(Character.toUpperCase(registrationDekanatRequest.getLastName().charAt(0)) + registrationDekanatRequest.getLastName().substring(1).toLowerCase());
        userService.createNewUserForDekanat(user);

        Dekanat dekanat = new Dekanat();
        dekanat.setUser(user);
        dekanat.setInstitute(educationService.getInstitute(instituteId));
        userService.saveDekanat(dekanat);

        return new SuccessfullResponse(HttpStatus.OK.value(), "Сотрудник дирекции успешно зарегистрирован");
    }
}
