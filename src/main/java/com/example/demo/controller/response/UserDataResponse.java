package com.example.demo.controller.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel (description = "Возвращает данные пользователя для реализации приложения на React")
public class UserDataResponse {

    @ApiModelProperty (value = "Идентификатор пользователя. Not null", example = "123")
    private int userId;

    @ApiModelProperty (value = "Роль пользователя в системе. Not null", example = "Студент")
    private String userRole;

    @ApiModelProperty (value = "Имя пользователя. Not null", example = "Иван")
    private String userFirstName;

    @ApiModelProperty (value = "Фамилия пользователя. Not null", example = "Иванов")
    private String userLastName;
}
