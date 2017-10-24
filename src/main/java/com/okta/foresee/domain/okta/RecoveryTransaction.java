package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by dhiraj.patil on 8/17/17.
 */
public class RecoveryTransaction implements Serializable {

    @JsonProperty("stateToken")
    public String stateToken;

    @JsonProperty("recoveryToken")
    public String recoveryToken;

    @JsonProperty("sessionToken")
    public String sessionToken;

    @JsonProperty("expiresAt")
    public String expiresAt;

    @JsonProperty("status")
    public String status;

    @JsonProperty("relayState")
    public String relayState;

    @JsonProperty("factortype")
    public FactorType factorType;

    @JsonProperty("recoveryType")
    public RecoveryType recoveryType;

    @JsonProperty("factorResult")
    public FactorResult factorResult;

    @JsonProperty("_embedded")
    public Object embedded;

    @JsonProperty("_links")
    public Object links;

}
