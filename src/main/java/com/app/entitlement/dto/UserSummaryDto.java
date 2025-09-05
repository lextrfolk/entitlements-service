package com.app.entitlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDto {
    private String id;
    private String username;
    private String email;
    private boolean enabled;

    public static UserSummaryDto from(UserRepresentation user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled()
        );
    }
}
