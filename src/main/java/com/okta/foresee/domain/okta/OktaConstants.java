package com.okta.foresee.domain.okta;

public interface OktaConstants {

    //Okta endpoints
    String OKTA_ENDPOINT_USERS = "/users";
    String OKTA_ENDPOINT_GROUPS = "/groups";
    String OKTA_USER_ID = "userId";
    String OKTA_GROUP_ID = "groupId";
    String OKTA_QUERY = "q";
    String OKTA_ENDPOINT_USER_ID = "/{" + OKTA_USER_ID + "}";
    String OKTA_ENDPOINT_GROUP_ID = "/{" + OKTA_GROUP_ID + "}";
    String OKTA_API_VERSION = "/api/v1";
    String OKTA_REQUEST_PARAM_ACTIVATE = "activate";
    String OKTA_REQUEST_PARAM_PROVIDER = "provider";
    String OKTA_ENDPOINT_LIFECYCLE = "/lifecycle";
    String OKTA_ACTIVATE = "/activate";
    String OKTA_UNSUSPEND = "/unsuspend";
    String OKTA_SUSPEND = "/suspend";
    String OKTA_UNLOCK = "/unlock";
    String OKTA_REQUEST_PARAM_SEND_EMAIL = "sendEmail";
    String OKTA_ENDPOINT_AUTHN = "/authn";
    String OKTA_ENDPOINT_CREDENTIALS = "/credentials";
    String OKTA_ENDPOINT_RESET_PASSWORD = "/reset_password";
    String AUTHORIZATION_HEADER = "Authorization";
    String OKTA_CONTENT_TYPE_HEADER = "application/json";
    String OKTA_ACCEPT_HEADER = "application/json";

    //Okta Authn endpoints
    String OKTA_ENDPOINT_RECOVERY = "/recovery";
    String OKTA_ENDPOINT_TOKEN = "/token";
}
