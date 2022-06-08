package com.example.demo.controller;

import com.example.demo.controller.request.ChangeRewardRequest;
import com.example.demo.controller.request.CreationRewardRequest;
import com.example.demo.controller.response.SuccessfullResponse;
import com.example.demo.entity.Achievement;
import com.example.demo.entity.File;
import com.example.demo.entity.Reward;
import com.example.demo.service.*;
import com.example.demo.view.AchievementsCreatedView;
import com.example.demo.view.RewardsView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Api(description = "Контроллер наград")
@RestController
public class RewardController {
    @Autowired
    private RewardService rewardService;
    @Autowired
    private FileService fileService;
    @Autowired
    private LogService logService;
    @Autowired
    private UserService userService;
    @Autowired
    private AchieveService achieveService;

    @ApiOperation("Список наград")
    @GetMapping("/rewards")
    public List<RewardsView> getAllRewards() {
        Reward blankReward = rewardService.getRewardByName("Без награды");
        return rewardService.getAllRewardWithoutBlankReward(blankReward.getIdReward(), RewardsView.class);
    }

    @ApiOperation("Получение конкретной награды")
    @GetMapping("/rewards/{rewardId}")
    public RewardsView getRewardById(@PathVariable
                                           @ApiParam (value = "Id награды. Not null.", example = "1")
                                                   Integer rewardId) {
        return rewardService.getRewardById(rewardId);
    }

    @ApiOperation("Создание новой награды - для админа")
    @PostMapping("/admin/createReward")
    public SuccessfullResponse createReward(@RequestBody CreationRewardRequest creationRewardRequest) {
        Reward reward = new Reward();
        Reward testReward = rewardService.getRewardByName(creationRewardRequest.getRewardName());

        if (testReward != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Награда с таким названием уже существует");
        }

        reward.setNameReward(creationRewardRequest.getRewardName());
        File file = new File();
        file.setDataFile(creationRewardRequest.getData());
        file.setFormatFile(creationRewardRequest.getFormat());
        file.setListFile(null);
        fileService.saveFile(file);

        reward.setFileOfReward(file);
        rewardService.saveReward(reward);
        logService.createNewLog(userService.getUserId(), 12, reward.getIdReward(), null, reward.getNameReward());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Награда успешно создана");
    }

    @ApiOperation("Удаление награды - для админа")
    @DeleteMapping("/admin/deleteReward/{rewardId}")
    public SuccessfullResponse deleteReward(
            @PathVariable
            @ApiParam(value = "Id награды. Not null. >0", example = "3") int rewardId
    ) {
        List<Achievement> findAchievement = achieveService.getAchieveByReward(rewardId, Achievement.class);

        if (!findAchievement.isEmpty()) {
            Map<Integer, String> achievementsNameList = new HashMap<>();

            for(Achievement achievement: findAchievement) {
                achievementsNameList.put(achievement.getIdAchieve(), achievement.getNameAchieve());
            }
            throw new ResponseStatusException(HttpStatus.FOUND, "Невозможно удалить награду, которая используется в достижениях: " + achievementsNameList);
        }
        Reward findReward = rewardService.getReward(rewardId);
        File rewardPicture = findReward.getFileOfReward();

        rewardService.deleteReward(findReward);
        fileService.deleteFile(rewardPicture);
        return new SuccessfullResponse(HttpStatus.OK.value(), "Награда успешно удалена");
    }

    @ApiOperation("Изменение данных о награде - для админа")
    @PutMapping("/admin/changeReward/{rewardId}")
    public SuccessfullResponse changeReward(@PathVariable
                                           @ApiParam(value = "Id награды. Not null. >0", example = "3")
                                                   int rewardId,
                                       @RequestBody
                                            @ApiParam(value = "Тело с изменяемыми параметрами")
                                            ChangeRewardRequest changeRewardRequest
                                       ) {
        Reward reward = rewardService.getReward(rewardId);
        if (reward==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Награда с указанным id не найдена");

        String oldData="";

        //Если название награды было передано, меняем его
        if (!changeRewardRequest.getRewardName().isEmpty())
        {
            oldData=changeRewardRequest.getRewardName().get();
            reward.setNameReward(changeRewardRequest.getRewardName().get());
        }
        //Если изображение или его формат были переданы, получаем файл и меняем указанные данные
        if (changeRewardRequest.getData().isEmpty() || !changeRewardRequest.getFormat().isEmpty()) {
            int fileId = reward.getFileOfReward().getIdFile();
            File file = fileService.getFileById(fileId, File.class);
            if (file==null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл с иконкой награды не найден");
            if (!changeRewardRequest.getData().isEmpty())
            {
                if (oldData.equals(""))
                    oldData= Arrays.toString(changeRewardRequest.getData().get());
                else
                    oldData=oldData+" "+ Arrays.toString(changeRewardRequest.getData().get());
                file.setDataFile(changeRewardRequest.getData().get());
            }
            if (!changeRewardRequest.getFormat().isEmpty())
            {
                if (oldData.equals(""))
                    oldData=changeRewardRequest.getFormat().get();
                else
                    oldData=oldData+" "+changeRewardRequest.getFormat();
                file.setFormatFile(changeRewardRequest.getFormat().get());
            }
            fileService.resetFile(file);
        }
        rewardService.saveReward(reward);
        logService.createNewLog(userService.getUserId(), 13, reward.getIdReward(), oldData, reward.getNameReward());
        return new SuccessfullResponse(HttpStatus.OK.value(), "Данные успешно изменены");
    }
}
