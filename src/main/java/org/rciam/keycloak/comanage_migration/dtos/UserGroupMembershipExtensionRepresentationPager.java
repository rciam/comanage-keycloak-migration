package org.rciam.keycloak.comanage_migration.dtos;

import java.util.List;

public class UserGroupMembershipExtensionRepresentationPager {

    public UserGroupMembershipExtensionRepresentationPager(){}

    private List<UserGroupMembershipExtensionRepresentation> results;
    private long count;

    public List<UserGroupMembershipExtensionRepresentation> getResults() {
        return results;
    }

    public void setResults(List<UserGroupMembershipExtensionRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
