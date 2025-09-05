package com.app.entitlement.service;

public interface PasswordService {

    void resetUserPassword(String userId, String newPassword, boolean temporary);

    void changePassword(String username, String currentPassword, String newPassword);

    void sendResetPasswordEmail(String userId);

    void requirePasswordUpdate(String userId);
}
