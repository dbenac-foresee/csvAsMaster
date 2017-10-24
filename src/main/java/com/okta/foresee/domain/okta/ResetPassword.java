package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by dhiraj.patil on 8/17/17.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "stateToken",
        "newPassword"
})
public class ResetPassword implements Serializable
{
    @JsonProperty("stateToken")
    public final String stateToken;
    @JsonProperty("newPassword")
    public final String newPassword;
}




