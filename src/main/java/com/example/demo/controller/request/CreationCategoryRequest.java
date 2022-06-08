package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel (description = "Данные, необходимые для создание новой награды")
public class CreationCategoryRequest {

    @ApiModelProperty (value = "Название категории. Not null", example = "ПГАС")
    String categoryName;

    @ApiModelProperty (value = "Иконка категории в виде byte[]. Not null")
    byte[] data;

    @ApiModelProperty (value = "Формат иконки категории. Not null", example = "png")
    String format;
}
