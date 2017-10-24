package com.okta.foresee.domain.okta;

/**
 * Created by dhiraj.patil on 9/14/17.
 */
public enum ProviderType {

    OKTA("okta"),
    ACTIVE_DIRECTORY("active_directory"),
    LDAP("ldap"),
    FEDERATION("federation"),
    SOCIAL("social");

    private String name;

    ProviderType(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ProviderType getByName(String name){
        return ProviderType.valueOf(name);
    }
}
