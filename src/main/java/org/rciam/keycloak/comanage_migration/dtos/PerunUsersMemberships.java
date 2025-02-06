package org.rciam.keycloak.comanage_migration.dtos;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class PerunUsersMemberships {

    private Map<String, PerunUserGroupMembershipRepresentation> users = new HashMap<>();

    public PerunUsersMemberships() {}

    @JsonAnySetter
    public void addUser(String username, PerunUserGroupMembershipRepresentation userMembership) {
        users.put(username, userMembership);
    }


    public Map<String, PerunUserGroupMembershipRepresentation> getUsers() {
        return users;
    }

    public void setUsers(Map<String, PerunUserGroupMembershipRepresentation> users) {
        this.users = users;
    }
}
