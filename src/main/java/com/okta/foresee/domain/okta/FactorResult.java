package com.okta.foresee.domain.okta;

/**
 * Created by dhiraj.patil on 8/17/17.
 */
public enum FactorResult {
    WAITING,    // Factor verification has started but not yet completed (e.g user hasn’t answered phone call yet)
    CANCELLED,  // Factor verification was canceled by user
    TIMEOUT,    // Unable to verify factor within the allowed time window
    TIME_WINDOW_EXCEEDED,   // Factor was successfully verified but outside of the computed time window. Another verification is required in current time window.
    PASSCODE_REPLAYED,  // Factor was previously verified within the same time window. OktaUser must wait another time window and retry with a new verification.
    ERROR;  // Unexpected server error occurred verifying factor
}
