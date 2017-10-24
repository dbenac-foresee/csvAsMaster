package com.okta.foresee.domain.okta;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum OktaGroupType {

    /**
     * Represents a client group in Okta.
     */
    CLIENT("client_");

    private String groupNamePrefix;
}
