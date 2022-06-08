package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel(description = "Запрос авторизации для студента через систему защиты ЭИОС")
public class AuthEIOSStudentRequest {

    @ApiModelProperty (value = "Логин студента в системе ЭИОС. Not null", example = "stud69854")
    private String login;

    @ApiModelProperty (value = "Пароль. Not null", example = "Hfoeu09804589")
    private String password;
}
