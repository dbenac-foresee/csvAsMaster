package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.*;
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
public class Profile implements Serializable {

    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String primaryPhone;
    private String profileUrl;
    private Long clientId;
    private Long foreseeId;
    private String externalId;
}
