package com.okta.foresee.csv;

public class ForeseeConstants {
	public static final String BLANK_STRING	=	"";
	public static final String OPEN_BRACE	=	"{";
	public static final String CLOSE_BRACE	=	"}";
	public static final String SQ_OPEN_BRACE	=	"[";
	public static final String SQ_CLOSE_BRACE	=	"]";
	public static final String COMA	=	",";
	public static final String QUESTION_MARK	=	"?";
	public static final String AT_SYMBOL	=	"@";
	public static final String FORESEE_ID	=	"ID";
	public static final String EXTERNAL_ID	=	"EXTERNAL_ID";
	public static final String CLIENT_ID	=	"CLIENT_ID";
	public static final String USERNAME	=	"USERNAME";
	public static final String USERNAME_SUFFIX	=	"USERNAME_SUFFIX";
	public static final String FIRST_NAME	=	"FIRST_NAME";
	public static final String LAST_NAME	=	"LAST_NAME";
	public static final String EMAIL	=	"EMAIL";
	public static final String NAME	=	"NAME";
	public static final String PHONE_NUMBER	=	"PHONE_NUMBER";
	public static final String ACCOUNT_ENABLED	=	"ACCOUNT_ENABLED";
	public static final String AUTHENTICATION_PROVIDER	=	"AUTHENTICATION_PROVIDER";
	public static final String USER_STATUS_ACTIVE	=	"Active";
	public static final String PROFILE	=	"profile";
	public static final String PROFILE_ID	=	"id";
	public static final String LOGIN	=	"login";
	public static final String STATUS	=	"status";
	public static final String USER_STATUS_PROVISIONED	=	"PROVISIONED";
	public static final String USER_STATUS_DEPROVISIONED	=	"DEPROVISIONED";
	public static final String USER_STATUS_SUSPENDED	=	"SUSPENDED";
	public static final String QUOTES	=	"\"";
	public static final String COLON	=	":";
	public static final String EXTERNALID	=	"externalID";
	public static final String FORESEEID	=	"foreseeID";
	public static final String CLIENTID	=	"clientID";
	public static final String FIRSTNAME	=	"firstName";
	public static final String LASTNAME	=	"lastName";
	public static final String PRIMARY_PHONE	=	"primaryPhone";
	public static final String FEDERATION	=	"FEDERATION";
	public static final String JSON_PROFILE_START	=	"profile\":{";
	public static final String CREDENTIALS_FEDERATION		=	",\"credentials\": {\"provider\": {\"type\": \"FEDERATION\",\"name\": \"FEDERATION\"}}";
	public static final String GROUP_IDS		=	"groupIds";
	public static final String STATUS_N	=	"N";
	public static final String STATUS_Y	=	"Y";
	

	public static final String GROUP_PREFIX	=	"client_";
	public static final String URL_SUFFIX_USERS	=	"/api/v1/users/";
	public static final String USER_SUFFIX_GROUP	=	"/groups";
	public static final String URL_SUFFIX_SUSPEND	=	"/lifecycle/suspend??sendEmail=false";
	public static final String URL_SUFFIX_UNSUSPEND	=	"/lifecycle/unsuspend?sendEmail=false";
	public static final String URL_SUFFIX_ACTIVATE	=	"/lifecycle/activate?sendEmail=false";
	public static final String URL_SUFFIX_DEACTIVATE	=	"/lifecycle/deactivate?sendEmail=false";
	public static final String USER_SEARCH_PATH	=	"/api/v1/users?search=";
	public static final String GROUP_SEARCH_PATH	=	"/api/v1/groups?q=";
	public static final String GROUP_URL_PREFIX	=	"/api/v1/groups/";
	public static final String USER_FIND_PATH	=	"profile.login eq ";


	public static final String AUTHZ_PROPERTY		=	"Authorization";
	public static final String SSWS		=	"SSWS ";
	public static final String CONTENT_TYPE		=	"Content-Type";
	public static final String APP_JSON		=	"application/json";
	public static final String ACCEPT		=	"Accept";
	public static final String METHOD_GET		=	"GET";
	public static final String METHOD_POST		=	"POST";
	public static final String METHOD_DELETE		=	"DELETE";
	

	public static final String RATE_LIMIT_REMAINING		=	"X-Rate-Limit-Remaining";
	public static final String RATE_LIMIT_RESET		=	"X-Rate-Limit-Reset";
	
	

	public static final String USER_SUCCESS_FILE="userSuccess.csv";
	public static final String USER_FAILURE_FILE="userFailure.csv";
	public static final String OTHER_SUCCESS_FILE="foreseeSuccess.csv";
	public static final String OTHER_FAILURE_FILE="foreseeFailure.csv";
	public static final String FORESEE_LOG_FILE="foreseeLog.csv";
	
}
