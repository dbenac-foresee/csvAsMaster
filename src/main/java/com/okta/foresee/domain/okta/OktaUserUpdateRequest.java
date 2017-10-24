package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Created by dhiraj.patil on 9/14/17.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OktaUserUpdateRequest {
    String id;
    UserStatus userStatus;
    Profile profile;
    Credentials credentials;
}
