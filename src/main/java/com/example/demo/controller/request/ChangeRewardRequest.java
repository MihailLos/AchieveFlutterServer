package com.example.demo.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
@ApiModel (description = "Данные для изменения награды")
public class ChangeRewardRequest {

    @ApiModelProperty (value = "Название награды. Может не передаваться", example = "Билет в кинотеатр")
    Optional<String> rewardName;

    @ApiModelProperty (value = "Иконка награды в виде byte[]. Может не передаваться")
    Optional<byte[]> data;

    @ApiModelProperty (value = "Формат иконки награды. Может не передаваться", example = "png")
    Optional<String> format;
}
