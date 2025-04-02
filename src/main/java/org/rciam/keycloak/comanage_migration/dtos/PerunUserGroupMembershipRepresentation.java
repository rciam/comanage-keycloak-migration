package org.rciam.keycloak.comanage_migration.dtos;

import java.util.List;

public class PerunUserGroupMembershipRepresentation {

    private String perunUserId;
    private String name;
    private PerunUserAttributes attributes;
    private List<String> groups;

    public PerunUserGroupMembershipRepresentation (){}

    public String getPerunUserId() {
        return perunUserId;
    }

    public void setPerunUserId(String perunUserId) {
        this.perunUserId = perunUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public PerunUserAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(PerunUserAttributes attributes) {
        this.attributes = attributes;
    }
}
