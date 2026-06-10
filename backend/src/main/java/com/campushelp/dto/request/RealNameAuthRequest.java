package com.campushelp.dto.request;

import lombok.Data;

@Data
public class RealNameAuthRequest {
    private String studentNo;
    private String name;
    private String idCardPhoto;
}
