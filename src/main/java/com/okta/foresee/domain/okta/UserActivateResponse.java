package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by dhiraj.patil on 9/14/17.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "activationUrl",
        "activationToken"
})
public class UserActivateResponse implements Serializable {
    public static final String AUDIT_SUBJECT_DISCRIMINATOR = "ACS-Users/OktaUserActivationResponse";
    public static final String AUDIT_SUBJECT_TYPE = "OktaUserActivationResponse";

    @JsonProperty("activationUrl")
    String activationUrl;
    @JsonProperty("activationToken")
    String activationToken;

}
