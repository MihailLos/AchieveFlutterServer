package com.example.demo.controller.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel (description = "Ответ, отправляемый при успешном выполнении запроса")
public class SuccessfullResponse {

    @ApiModelProperty (value = "Статус-код. Not null")
    private int status;

    @ApiModelProperty (value = "Сообщение. Not null")
    private String message;
}
