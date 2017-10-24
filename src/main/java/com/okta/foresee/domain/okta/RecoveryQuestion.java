package com.okta.foresee.domain.okta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by dhiraj.patil on 8/14/17.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "question",
        "answer"
})
public class RecoveryQuestion implements Serializable
{
    @JsonProperty("question")
    public String question;
    @JsonProperty("answer")
    public String answer;
}
