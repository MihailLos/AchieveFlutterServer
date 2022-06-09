package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

@Data
@AllArgsConstructor
@ApiModel (description = "Запрос с данными для заявки на изменение достижения в системе")
public class EditAchieveForAdminRequest {

    @ApiModelProperty (value = "Название достижения. Null", example = "Оратор")
    private Optional<String> achieveName;

    @ApiModelProperty (value = "Описание достижения. Null", example = "Принять участие в научной конференции")
    private Optional<String> achieveDescription;

    @ApiModelProperty (value = "Баллы, начисляемые за выполнение достижения. Null", example = "15")
    private Optional<Integer> achieveScore;

    @ApiModelProperty (value = "Дата, начиная с которой можно получить достижение. Null", example = "2021-01-01")
    private Optional<LocalDate> achieveStartDate;

    @ApiModelProperty (value = "Дата, до которой можно получить достижение. Может быть null", example = "2021-01-26")
    private Optional<LocalDate> achieveEndDate;

    @ApiModelProperty (value = "Id категории достижения. Null", example = "3")
    private Optional<Integer> categoryId;

    @ApiModelProperty (value = "Изображение достижения в виде byte[]. Null")
    private Optional<byte[]> photo;

    @ApiModelProperty (value = "Формат изображения достижения. Null", example = "png")
    private Optional<String> format;

    @ApiModelProperty (value = "Id награды за выполнение достижения. Null", example = "4")
    private Optional<Integer> rewardId;

    @ApiModelProperty (value = "Статус активности достижения. Null", example = "4")
    private Optional<Integer> statusActiveId;
}
