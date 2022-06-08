package com.example.demo.view;

import com.example.demo.entity.Institute;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

@ApiModel(description = "Список модераторов")
public interface ModersView {
    @JsonProperty("moderId")
    int getId();

    @ApiModelProperty(value = "Данные о пользователе")
    UserView getUser();
    interface UserView {
        @ApiModelProperty(value = "Id пользователя. Not null. >0", example = "27")
        @JsonProperty("userId")
        int getId();

        @ApiModelProperty(value = "Имя пользователя. Not null", example = "Егор")
        String getFirstName();

        @ApiModelProperty(value = "Фамилия пользователя. Not null", example = "Панфилов")
        String getLastName();
    }

    Set<Integer> getInstitutesId();
}
