package org.rciam.keycloak.comanage_migration.dtos;

import java.util.List;

public class PerunUserGroupMembershipRepresentation {

    private String userid;
    private String name;
    private List<String> groups;

    public PerunUserGroupMembershipRepresentation (){}

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
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
}
