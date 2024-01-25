package com.mirrors.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserDTO {
    
    private Long id;

    private String nickName;

    private String icon;
}
