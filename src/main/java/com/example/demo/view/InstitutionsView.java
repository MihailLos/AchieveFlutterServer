package com.example.demo.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel (description = "Список всех институтов")
public interface InstitutionsView {

    @ApiModelProperty (value = "Id института. Not null.", example = "390")
    @JsonProperty("instituteId")
    int getId();

    @ApiModelProperty (value = "Полное название института. Not null", example = "Институт фундаментальных наук")
    String getInstituteFullName();
}
