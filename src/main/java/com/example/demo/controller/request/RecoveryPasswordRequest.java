package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel(description = "Запрос с данными для восстановления пароля")
public class RecoveryPasswordRequest {

    /*  Данные пользователя  */
    @ApiModelProperty(value = "Электронная почта пользователя. Not null", example = "typ89@yandex.ru")
    private String email;

    @ApiModelProperty(value = "Новый пароль. Not null. Не менее 8 символов", example = "fd98TbkK13")
    private String password;
}
