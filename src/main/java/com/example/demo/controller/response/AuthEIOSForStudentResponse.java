package com.example.demo.controller.response;

import com.example.demo.entity.File;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel(description = "Ответ, отправляемый при успешной авторизации студента через систему ЭИОС")
public class AuthEIOSForStudentResponse {

    @ApiModelProperty (value = "Токен доступа ИС учета достижений студентов. Not null", example = "ewjewreujgwvnmrnbwnorebnfeobeibonre")
    private String accessToken;

    @ApiModelProperty (value = "Рефреш токен ИС учета достижений студентов. Not null", example = "ewjewreujgwvnmrnbwnorebnfeobeibonre")
    private String refreshToken;
}
