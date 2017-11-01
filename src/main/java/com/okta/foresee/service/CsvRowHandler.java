package com.okta.foresee.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.okta.foresee.domain.ForeseeConstants;
import com.okta.foresee.domain.okta.*;
import com.okta.foresee.util.ForeseeCSVUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.okta.foresee.csv.CsvImporter.groupsMap;
import static com.okta.foresee.domain.ForeseeConstants.*;

@Slf4j
@Service
public class CsvRowHandler {
    
    @Autowired
    private OktaService oktaService;

    @Autowired
    private ForeseeCSVUtil foreseeCSVUtil;

    public void saveUser(CSVRecord csvLine) {

        final String foreseeId = csvLine.get(FORESEE_ID).trim();
        final String userName = csvLine.get(USERNAME).trim();
        final String userNameSuffix = csvLine.get(USERNAME_SUFFIX).trim();
        //Check if the username is in email format, otherwise append suffix
        String loginName = oktaService.getOktaLogin(userName, userNameSuffix);
        final String authnProvider = csvLine.get(AUTHENTICATION_PROVIDER).trim();
        final boolean accountEnabled = "Y".equalsIgnoreCase(csvLine.get(ACCOUNT_ENABLED).trim());
        final String clientId = csvLine.get(CLIENT_ID).trim();
        final String email = csvLine.get(EMAIL).trim();
        final String externalId = csvLine.get(EXTERNAL_ID).trim();
        final String phone = csvLine.get(PHONE_NUMBER).trim();
        boolean clientIdChange = false;

        String groupName = ForeseeConstants.GROUP_PREFIX + csvLine.get(CLIENT_ID);

        OktaUser.OktaUserBuilder userBuilder = OktaUser.builder();

        Profile profile = Profile.builder()
                .foreseeId(Long.valueOf(foreseeId))
                .externalId(Strings.isNullOrEmpty(externalId) ? null : externalId)
                .clientId(Strings.isNullOrEmpty(clientId) ? null : Long.valueOf(clientId))
                .login(loginName)
                .firstName(csvLine.get(FIRST_NAME).trim())
                .lastName(csvLine.get(LAST_NAME).trim())
                .email(email)
                .primaryPhone(Strings.isNullOrEmpty(phone) ? null : phone)
                .build();

        userBuilder.profile(profile);
        userBuilder.groupIds(Lists.newArrayList(
                groupsMap.get(ForeseeConstants.GROUP_PREFIX + csvLine.get(CLIENT_ID)).getId()));

        if (ForeseeConstants.FEDERATION.equals(authnProvider)) {
            userBuilder.credentials(Credentials.builder()
                    .provider(Provider.builder()
                            .name(ForeseeConstants.FEDERATION)
                            .type(ProviderType.FEDERATION)
                            .build())
                    .build());
        }

        OktaUser oktaUserToUpdate = userBuilder.build();


        //check if all the required ATTRIBUTES are not null/blank
        if (Strings.isNullOrEmpty(userName)
                || Strings.isNullOrEmpty(oktaUserToUpdate.getProfile().getFirstName())
                || Strings.isNullOrEmpty(oktaUserToUpdate.getProfile().getLastName())
                || !foreseeCSVUtil.isValidUserNameFormat(oktaUserToUpdate.getProfile().getEmail())
                || !foreseeCSVUtil.isValidUserNameFormat(oktaUserToUpdate.getProfile().getLogin())
                || oktaUserToUpdate.getProfile().getClientId() == null) {
            log.debug("recordFailure missing mandatory ATTRIBUTES------");
            foreseeCSVUtil.writeToFile("UserName: " + userName + ", " + "FN_LN_REQID_MISSING or EMAIL is not in format" + "," + oktaUserToUpdate,
                    ForeseeConstants.USER_FAILURE_FILE);
            return;//exit if these fields are blank
        }

        boolean isCreate = false;
        OktaUser existingOktaUser;

        //identify create or update scenario & generate unique user name for create
        existingOktaUser = oktaService.searchForExistingUser(loginName);

        if (existingOktaUser == null) {
            log.debug(" user  doesn't exist: {} -------Create", loginName);
            isCreate = true;
        } else {
            log.debug(" user exists ---------" + loginName + "-------Updating");
            if (!oktaUserToUpdate.getProfile().getForeseeId().equals(existingOktaUser.getProfile().getForeseeId())) {
                log.debug("Skipping user {}. The user found in Okta has a different Foresee Id than the user from the csv file", userName);
                foreseeCSVUtil.writeToFile(foreseeId + "," +
                                userName + ", " +
                                "foreseeId doesn't match the value in Okta:" + existingOktaUser.getProfile().getForeseeId(),
                        ForeseeConstants.USER_FAILURE_FILE);
                return; //don't process if the foresee id not matched - as per 10/9 discussion
            }
            oktaUserToUpdate.setGroupIds(null);
            oktaUserToUpdate.setId(existingOktaUser.getId());
            clientIdChange = !clientId.equals(existingOktaUser.getProfile().getClientId().toString());
        }

        //Create or update call
        existingOktaUser = oktaService.saveUser(oktaUserToUpdate);

        if (existingOktaUser == null) {
            return;
        }

        UserStatus existingUserStatus = existingOktaUser.getStatus();

        //Post create/update user LCM operation - suspend, unsuspend, deactivate
        String userOperationLog = oktaUserToUpdate.getProfile().getForeseeId() + "," + existingOktaUser.getId() + ",PROFILE_MIGRATED";
        foreseeCSVUtil.writeToFile(userOperationLog, USER_ID_LOG_FILE);

        if (!accountEnabled && UserStatus.SUSPENDED != existingUserStatus) {
            oktaService.suspendUser(existingOktaUser);
            existingUserStatus = UserStatus.SUSPENDED;
        } else if (accountEnabled && UserStatus.SUSPENDED == existingUserStatus) {
            oktaService.unsuspendUser(existingOktaUser);
            existingUserStatus = UserStatus.ACTIVE;
        } else if (accountEnabled && UserStatus.DEPROVISIONED == existingUserStatus ) {
            oktaService.activateUser(existingOktaUser);
            existingUserStatus = UserStatus.ACTIVE;
        }

        if (!isCreate) { //update scenario - Update group membership, Update Authentication provider

            if (clientIdChange) {
                OktaGroup oktaGroup = oktaService.getUserGroups(existingOktaUser.getId());

                if (oktaGroup != null && !oktaGroup.getProfile().getName().equals(groupName)) {
                    oktaService.removeUserFromGroup(existingOktaUser, oktaGroup);
                    oktaService.addUserToGroup(existingOktaUser, groupsMap.get(groupName));
                }
            }

            String updatedAuthProvider = !authnProvider.equals(ForeseeConstants.FEDERATION) ? "OKTA" : authnProvider;

            if (!existingOktaUser.getCredentials().getProvider().getName().equals(updatedAuthProvider)
                    && (existingUserStatus == UserStatus.ACTIVE || existingUserStatus == UserStatus.PROVISIONED)) {
                oktaService.flipFederation(
                        existingOktaUser, ForeseeConstants.FEDERATION.equals(updatedAuthProvider));
            }
        }
    }

    public void deleteUser(CSVRecord csvLine) {

        String userName = csvLine.get(USERNAME).trim();
        String userNameSuffix = csvLine.get(USERNAME_SUFFIX).trim();

        String loginName = oktaService.getOktaLogin(userName, userNameSuffix);

        OktaUser existingOktaUser = oktaService.searchForExistingUser(loginName);

        if (existingOktaUser != null) {
            oktaService.deactivateUser(existingOktaUser);
            oktaService.deleteUser(existingOktaUser);
        }
    }
}
