package com.example.demo.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Список статусов активности пользотваелей")
public interface StatusActiveUserView {

    @ApiModelProperty (value = "Id статуса активности пользотваеля. Not null.", example = "3")
    @JsonProperty("statusActiveUserId")
    int getId();

    @ApiModelProperty (value = "Статус активности пользователя", example = "Активен")
    @JsonProperty("statusActiveUserName")
    String getStatusUser();
}
