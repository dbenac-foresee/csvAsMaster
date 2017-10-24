package com.okta.foresee.domain.okta;

/**
 * Created by dhiraj.patil on 8/14/17.
 */

/**
 * OKTA User status (Different from OKTA_STATUS attribute of User in db)
 */
public enum UserStatus {
    STAGED, PROVISIONED, ACTIVE, RECOVERY, LOCKED_OUT, PASSWORD_EXPIRED, SUSPENDED, DEPROVISIONED

}
