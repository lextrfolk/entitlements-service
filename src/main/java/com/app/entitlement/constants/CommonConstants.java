package com.app.entitlement.constants;

public interface CommonConstants {

    String FAILURE = "Failure";
    String SUCCESS = "Success";


    String STATUS = "status";

    // Below are the endpoints for ProvisioningController

    String ENTITLEMENT_BASE_PATH = "/api/entitlements" ;

    String SAVE_METADATA_API = "/metadata";


    // Below are the endpoints for UserController
    String USER_BASE_PATH = "/api/users";
    String PASSWORD_BASE_PATH = "/api/passwords";
}
