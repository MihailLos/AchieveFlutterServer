package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
@ApiModel(description = "Запрос на изменение категории")
public class ChangeCategoryRequest {

    @ApiModelProperty (value = "Название категории. Может не передаваться, тогда эти данные не будут изменены. Если передается -  Not null", example = "Пример названия")
    private Optional<String> categoryName;

    @ApiModelProperty (value = "Иконка категории в виде byte[]. Может не передаваться, тогда эти данные не будут изменены. Если передается -  Not null", example = "Hvzfddsf")
    private Optional<byte[]> fileData;

    @ApiModelProperty (value = "Формат иконки категории. Может не передаваться, тогда эти данные не будут изменены. Если передается -  Not null", example = "png")
    private Optional<String> fileFormat;
}
