package com.okta.foresee.domain;

import lombok.Data;

@Data
public class ApiResponse {
	public String exception;
    public int responseCode;
    public String errorOutput;
    public String output;
    public int rateLimit;
    public Boolean emptyOutput;
}
