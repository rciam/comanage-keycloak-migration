package org.rciam.keycloak.comanage_migration.comanage;

import org.rciam.keycloak.comanage_migration.dtos.GroupEnrollmentConfigurationRepresentation;

import java.util.List;

public class ComanageGroupRepresentation {

    public ComanageGroupRepresentation(){}

    private String name;
    private String description;
    private String parentName;
    private List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<GroupEnrollmentConfigurationRepresentation> getEnrollmentConfigurationList() {
        return enrollmentConfigurationList;
    }

    public void setEnrollmentConfigurationList(List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList) {
        this.enrollmentConfigurationList = enrollmentConfigurationList;
    }
}
