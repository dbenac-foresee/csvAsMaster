package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by dhiraj.patil on 8/14/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Credentials implements Serializable
{
    public Password password;
    public RecoveryQuestion recoveryQuestion;
    public Provider provider;
}