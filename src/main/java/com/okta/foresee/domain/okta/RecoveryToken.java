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
        "recoveryToken",
})
public class RecoveryToken implements Serializable
{
    @JsonProperty("recoveryToken")
    public final String recoveryToken;
}
