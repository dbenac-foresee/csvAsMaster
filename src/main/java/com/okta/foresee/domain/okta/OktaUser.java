package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by dhiraj.patil on 8/14/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OktaUser implements Serializable {

    String id;
    Date created;
    Date activated;
    Date statusChanged;
    Date lastLogin;
    Date lastUpdated;
    Date passwordChanged;
    UserStatus status;
    Profile profile;
    Credentials credentials;

    List<String> groupIds;

}
