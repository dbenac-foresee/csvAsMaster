package com.okta.foresee.domain.okta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a group in Okta.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OktaGroup  {

    /**
     * The id of the group.
     */
    private String id;

    /**
     * The profile containing the information about the group.
     */
    private GroupProfile profile;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GroupProfile {

        /**
         * The name of the group.
         */
        private String name;

        /**
         * The description of the group.
         */
        private String description;
    }
}
