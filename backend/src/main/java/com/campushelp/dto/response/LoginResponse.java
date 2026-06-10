package com.campushelp.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer isRealname;
    private Integer creditScore;
    private String roles;
}
