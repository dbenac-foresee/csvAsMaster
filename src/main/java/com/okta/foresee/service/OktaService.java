package com.okta.foresee.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.okta.foresee.config.AppProperties;
import com.okta.foresee.domain.ApiResponse;
import com.okta.foresee.domain.ForeseeConstants;
import com.okta.foresee.domain.okta.OktaGroup;
import com.okta.foresee.domain.okta.OktaUser;
import com.okta.foresee.util.ForeseeCSVUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.okta.foresee.domain.ForeseeConstants.*;

/**
 * @author Sivaji Sabbineni
 */
@Slf4j
@Component
public class OktaService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private OktaConnectionHelper oktaConnectionHelper;

    @Autowired
    private ForeseeCSVUtil foreseeCSVUtil;

    public Map<String, OktaGroup> getAllGroups() {
        String path = appProperties.getOktaUrl() + "/api/v1/groups";;

        log.debug("Calling to get all groups. Url={}", path);

        ApiResponse ret = oktaConnectionHelper.get(path);

        HashMap<String, OktaGroup> groupsMap = new HashMap<>();

        if (ret.exception != null) {
            log.debug(path + "," + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200) {
            try {
                List<OktaGroup> oktaGroups = objectMapper.readValue(
                        ret.output, new TypeReference<List<OktaGroup>>() {});
                for (OktaGroup oktaGroup : oktaGroups) {
                    groupsMap.put(oktaGroup.getProfile().getName(), oktaGroup);
                }
            } catch (IOException e) {
                log.error("Failed to parser json", e);
                throw new RuntimeException(e);
            }
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(path + ", GetAllGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(path + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(path + ", GetAllGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
        }

        return groupsMap;
    }

    public OktaGroup getUserGroups(String userName) {

        String path;

        try {
            path = appProperties.getOktaUrl() +
                    ForeseeConstants.URL_SUFFIX_USERS +
                    java.net.URLEncoder.encode(userName, "UTF-8") +
                    ForeseeConstants.USER_SUFFIX_GROUP;
        } catch (UnsupportedEncodingException e1) {
            log.error("Exception trying to encode url.", e1);
            throw new RuntimeException(e1);
        }

        log.debug("Calling to get groups for user. Url={}", path);

        ApiResponse ret = oktaConnectionHelper.get(path);

        if (ret.exception != null) {
            log.debug(userName + "," + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200) {
            List<OktaGroup> oktaGroups = Lists.newArrayList();
            try {
                oktaGroups.addAll(objectMapper.readValue(ret.output, new TypeReference<List<OktaGroup>>() {}));
            } catch (IOException e) {
                log.error("Failed to parse json.", e);
                throw new RuntimeException(e);
            }
            for (OktaGroup oktaGroup : oktaGroups) {
                if (!"Everyone".equals(oktaGroup.getProfile().getName())) {
                    log.debug("Returning group {}", oktaGroup.getProfile().getName());
                    return oktaGroup;
                }
            }
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(userName + ", GetGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(userName + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(userName + ", GetGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
        }
        return null;
    }

    public OktaGroup createGroup(String groupName, String groupDescription) {
        OktaGroup oktaGroup = OktaGroup.builder()
                .profile(OktaGroup.GroupProfile.builder()
                        .name(groupName)
                        .description(groupDescription)
                        .build())
                .build();
        return saveGroup(oktaGroup);
    }

    public OktaGroup saveGroup(OktaGroup oktaGroup) {

        String path;
        String groupName = oktaGroup.getProfile().getName();

        path = appProperties.getOktaUrl() + "/api/v1/groups";
        log.debug("Saving OktaGroup with name '{}'", groupName);

        ApiResponse ret;
        try {
            ret = oktaConnectionHelper.post(path, objectMapper.writeValueAsString(oktaGroup));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Json.", e);
            throw new RuntimeException(e);
        }

        if (ret.exception != null) {
            log.debug(groupName + "," + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200) {
            foreseeCSVUtil.writeToFile(groupName + ",CREATE," + path + "," + ret.output, OPERATION_SUCCESS_FILE);
            try {
                OktaGroup updatedOktaGroup = objectMapper.readValue(ret.output, OktaGroup.class);
                log.debug("Found Okta group with id '{}'", updatedOktaGroup.getId());
                return updatedOktaGroup;
            } catch (IOException e) {
                log.error("Failed to parse Json.", e);
            }
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput + "," + path, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(groupName + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput + "," + path, ForeseeConstants.OTHER_FAILURE_FILE);
        }
        return null;
    }

    public void activateUser(OktaUser oktaUser) {
        log.debug("Activating user {}", oktaUser.getProfile().getLogin());

        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId() + ForeseeConstants.URL_SUFFIX_ACTIVATE;
        wrapperLCMPost(url, oktaUser);
    }

    public void suspendUser(OktaUser oktaUser) {
        log.debug("Suspending user {}", oktaUser.getProfile().getLogin());

        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId() + URL_SUFFIX_SUSPEND;
        wrapperLCMPost(url, oktaUser);
    }

    public void unsuspendUser(OktaUser oktaUser) {
        log.debug("Un-Suspending user {}", oktaUser.getProfile().getLogin());

        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId() + URL_SUFFIX_UNSUSPEND;
        wrapperLCMPost(url, oktaUser);
    }

    public void deactivateUser(OktaUser oktaUser) {
        String username = oktaUser.getProfile().getLogin();
        log.debug("De-activating user {}", username);

        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId() + ForeseeConstants.URL_SUFFIX_DEACTIVATE;
        wrapperLCMPost(url, oktaUser);
    }

    /**
     * Wrapper for lifecycle management post operations.
     *
     * @param url the url in which to post
     * @param oktaUser the user to affect
     */
    private void wrapperLCMPost(String url, OktaUser oktaUser) {
        String username = oktaUser.getProfile().getLogin();
        log.debug("------------wrapperLCMPost-------" + url);
        ApiResponse ret = oktaConnectionHelper.post(url, ForeseeConstants.BLANK_STRING);

        if (!Strings.isNullOrEmpty(ret.output)) {
            log.debug("Response from lcm post: {}", ret.output);
        }

        if (ret.exception != null) {
            log.debug("LCM operation exception for " + username + "   " + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
            log.debug("User lifecycle management operation completed successfully. Url={}", url);
            foreseeCSVUtil.writeToFile(username + ",LCM," + url, OPERATION_SUCCESS_FILE);
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(username + ", LCM, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(username + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(username + ", LCM, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
        }
    }

    public void deleteUser(OktaUser oktaUser) {
        String username = oktaUser.getProfile().getLogin();
        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId();
        log.debug("Deleting user {}", username);
        ApiResponse ret = oktaConnectionHelper.delete(url, ForeseeConstants.BLANK_STRING);

        if (ret.exception != null) {
            log.debug("LCM operation exception for " + username + "   " + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
            log.debug("User deleted: {}", username);
            foreseeCSVUtil.writeToFile(username + ",DELETE, " + url, OPERATION_SUCCESS_FILE);
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(username + ", LCM, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(username + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(username + ", LCM, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
        }
    }

    public OktaUser saveUser(OktaUser oktaUser) {
        String path;
        String createUpdate = "Create";
        boolean createUser = Strings.isNullOrEmpty(oktaUser.getId());

        path = appProperties.getOktaUrl() + "/api/v1/users";

        if (createUser) {//create scenario since okta record is not created yet
            path = path + "?activate=false&sendEmail=false&provider=true";
            log.debug("Attempting user create for user {}", oktaUser.getProfile().getLogin());
        } else {//update scenario
            path = path + "/" + oktaUser.getId();
            createUpdate = "Update";
            log.debug("Attempting user update for user {}", oktaUser.getProfile().getLogin());
        }

        ApiResponse ret;
        try {
            ret = oktaConnectionHelper.post(path, objectMapper.writeValueAsString(oktaUser));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Json.", e);
            throw new RuntimeException(e);
        }

        OktaUser updatedOktaUser = null;
        if (ret.output != null) {
            try {
                updatedOktaUser = objectMapper.readValue(ret.output, OktaUser.class);
            } catch (IOException e) {
                log.error("Error parsing json", e);
                throw new RuntimeException(e);
            }
            log.debug("User saved, {}", updatedOktaUser.getProfile().getLogin());
        }

        if (ret.exception != null) {
            log.debug(oktaUser.getId() + "," + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200) {
            foreseeCSVUtil.writeToFile(updatedOktaUser.getProfile().getForeseeId() + ","
                    + updatedOktaUser.getId() + ","
                    + ret.output, ForeseeConstants.USER_SAVE_SUCCESS_FILE);
            if (createUser) {
                activateUser(updatedOktaUser);
            }
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(oktaUser.getProfile().getLogin() + ", " + createUpdate + ", " + ret.responseCode + ", " + ret.errorOutput + "," + oktaUser, ForeseeConstants.USER_FAILURE_FILE);
            log.debug(oktaUser.getId() + ", Rate Limit Hit, TRY AGAIN");
        } else {
            log.debug("Failed UID----------------------" + oktaUser.getId() + "------" + oktaUser);
            log.debug(oktaUser.getId() + ", " + createUpdate + ", " + ret.responseCode + ", " + ret.errorOutput);
            foreseeCSVUtil.writeToFile(oktaUser.getId() + ", " + createUpdate + ", " + ret.responseCode + ", " + ret.errorOutput + "," + oktaUser, ForeseeConstants.USER_FAILURE_FILE);
        }
        return updatedOktaUser;
    }

    /*
     * Add user to group
     */
    public void addUserToGroup(OktaUser oktaUser, OktaGroup oktaGroup) {

        String uid = oktaUser.getId();

        String url = appProperties.getOktaUrl() + ForeseeConstants.URL_SUFFIX_GROUPS + oktaGroup.getId() + "/users/" + uid;

        ApiResponse ret = oktaConnectionHelper.put(url, ForeseeConstants.BLANK_STRING);

        if (ret.exception != null) {
            log.debug("addUserToGroup operation exception for " + uid + "   " + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
            log.debug("User {} added to group {}", oktaUser.getProfile().getLogin(), oktaGroup.getProfile().getName());
            foreseeCSVUtil.writeToFile(oktaUser.getProfile().getLogin() + ",ADD GROUP," + oktaGroup.getProfile().getName(), OPERATION_SUCCESS_FILE);
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(uid + ", ADD Group, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(uid + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(uid + ", ADD Group, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
        }
    }

    public void removeUserFromGroup(OktaUser oktaUser, OktaGroup oktaGroup) {

        String uid = oktaUser.getId();
        String username = oktaUser.getProfile().getLogin();
        String groupName = oktaGroup.getProfile().getName();

        String url = appProperties.getOktaUrl() + ForeseeConstants.URL_SUFFIX_GROUPS + oktaGroup.getId() + "/users/" + uid;

        ApiResponse ret = oktaConnectionHelper.delete(url, "");

        if (ret.exception != null) {
            log.debug("removeUserFromGroup operation exception for " + uid + "   " + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
            log.debug("User '{}' removed from group '{}'", username, groupName);
            foreseeCSVUtil.writeToFile(username + ",REMOVE GROUP," + groupName, OPERATION_SUCCESS_FILE);
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(uid + ", REMOVE Group, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(uid + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(uid + ", REMOVE Group, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
        }
    }

    public void flipFederation(OktaUser oktaUser, boolean toFederation) {

        String url = appProperties.getOktaUrl() + URL_SUFFIX_USERS + oktaUser.getId() + URL_SUFFIX_RESET_PASSWORD;
        String username = oktaUser.getProfile().getLogin();

        if (toFederation) {
            url += "&provider=FEDERATION";
        }

        log.debug("Flipping federation state. Url={}", url);
        ApiResponse ret = oktaConnectionHelper.post(url, "");

        if (ret.exception != null) {
            log.debug("flipFederation operation exception for " + username + "   " + ret.exception + ", TRY AGAIN");
        } else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
            log.debug("Federation state changed. Federate={}", toFederation);
            foreseeCSVUtil.writeToFile(username + ",SET FEDERATION," + toFederation + "," + url, OPERATION_SUCCESS_FILE);
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(username + ", flipFederation, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(username + ", Rate Limit Hit, TRY AGAIN");
        } else {
            foreseeCSVUtil.writeToFile(username + ", flipFederation, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
        }
    }

    public OktaUser searchForExistingUser(String username) {

        OktaUser userObject = null;

        String url;
        try {
            url = appProperties.getOktaUrl() + ForeseeConstants.URL_SUFFIX_USERS + java.net.URLEncoder.encode(username, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode the path.", e);
            return null;
        }

        log.debug("Searching for user {} at {}", username, url);
        ApiResponse ret = oktaConnectionHelper.get(url);

        if (ret.responseCode == 200 && ret.emptyOutput.equals(Boolean.FALSE)) {
            try {
                userObject = objectMapper.readValue(ret.output, OktaUser.class);
            } catch (Exception e) {
                log.error("Failed to parse Json.", e);
            }
        } else if (ret.responseCode == 429) {
            foreseeCSVUtil.writeToFile(username + ",SEARCH, " + ret.responseCode + ", " + ret.errorOutput + "," + url, ForeseeConstants.OTHER_FAILURE_FILE);
            log.debug(username + ", Rate Limit Hit, TRY AGAIN");
        } else {
            log.debug("No user found with login {}", username);
            return null;
        }

        return userObject;
    }

    public String getOktaLogin(String userName, String userNameSuffix) {
        return foreseeCSVUtil.isValidUserNameFormat(userName)
                ? userName.toLowerCase().trim()
                : (userName.trim() + ForeseeConstants.AT_SYMBOL + userNameSuffix.trim()).toLowerCase();
    }
}
