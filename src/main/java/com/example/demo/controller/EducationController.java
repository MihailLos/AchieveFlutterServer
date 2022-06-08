package com.example.demo.controller;

import com.example.demo.controller.request.CreationGroupRequest;
import com.example.demo.controller.request.CreationStreamRequest;
import com.example.demo.entity.*;
import com.example.demo.view.*;
import com.example.demo.service.EducationService;
import com.example.demo.service.LogService;
import com.example.demo.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(description = "Контроллер данных об образовании")
@RestController
public class EducationController {
    @Autowired
    private EducationService educationService;
    @Autowired
    private LogService logService;
    @Autowired
    private UserService userService;

    @ApiOperation("Список форм обучения")
    @GetMapping("/education/formEducation")
    public List<FormEducationVew> getAllFormEducation() {
        return educationService.getAllFormEducation(FormEducationVew.class);
    }

    @ApiOperation("Список институтов")
    @GetMapping("/education/institutions")
    public List<InstitutionsView> getAllInstitute() {
        return educationService.getAllInstitute(InstitutionsView.class);
    }

    @ApiOperation("Список направлений раннее выбранного института")
    @GetMapping("/education/stream/{instituteId}")
    public List<StreamView> getStreamForInstitute(@PathVariable
                                       @ApiParam (value = "Id раннее выбранного института. Not null. [1,15]", example = "11")
                                               Integer instituteId) {
        Institute institute = educationService.getInstitute(instituteId);
        if (institute==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Институт с указанным id не найден");
        return educationService.getStreamInstitute(instituteId, StreamView.class);
    }

    @ApiOperation("Список групп раннее выбранного направления")
    @GetMapping("/education/group/{streamId}")
    public List<GroupView> getGroupForStream(@PathVariable
                                            @ApiParam (value = "Id раннее выбранного направления. Not null. >0", example = "3")
                                                    int streamId) {
        Stream stream = educationService.getStream(streamId);
        if (stream==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Направление с указанным id не найдено");
        return educationService.getGroupsForStream(streamId, GroupView.class);
    }

    @ApiOperation("Получение доступных институтов для модератора")
    @GetMapping("/moderator/getAvailableInstitutes")
    public List<InstitutionsView> getAvailableInstitutesList()  {
        User currentUser = userService.getUser();
        Moderator currentModerator = userService.getModeratorByUserId(currentUser.getIdUser(), Moderator.class);
        return educationService.getAvailableInstitutesListForModer(currentModerator, InstitutionsView.class);
    }

    @ApiOperation("Получение институтов конкретного модератора - для админа")
    @GetMapping("/admin/getModeratorInstitutes/{userId}")
    public List<InstitutionsView> getAvailableInstitutesList(
            @PathVariable
            @ApiParam(value = "id пользователя в роли Модератор Not null. >0", example = "4")
                    int userId
    ) {
        Moderator findModerator = userService.getModeratorByUserId(userId, Moderator.class);
        return educationService.getAvailableInstitutesListForModer(findModerator, InstitutionsView.class);
    }

}
