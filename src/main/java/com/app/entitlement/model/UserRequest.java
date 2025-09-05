package com.app.entitlement.model;

import lombok.Data;

@Data
public class UserRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String key;
    private String userName;
}
