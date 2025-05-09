package org.rciam.keycloak.comanage_migration;

import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.rciam.keycloak.comanage_migration.keycloak.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ShellComponent
public class MigrationCommands {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakConfig keycloakConfig;

    @Autowired
    public MigrationCommands(KeycloakAdminService keycloakAdminService, KeycloakConfig keycloakConfig) {
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakConfig = keycloakConfig;
    }

    @ShellMethod("Create users in Keycloak from JSON file")
    public void createUsers(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processUsersFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
            clientId != null ? clientId : keycloakConfig.getClientId(), 
            clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }

    @ShellMethod("Create groups and related configuration in Keycloak from JSON file")
    public void createGroups(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processGroupsFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
                clientId != null ? clientId : keycloakConfig.getClientId(),
                clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }


    @ShellMethod("Create group admins in Keycloak from JSON file")
    public void createGroupAdmins(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processGroupAdminsFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
                clientId != null ? clientId : keycloakConfig.getClientId(),
                clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }

    @ShellMethod("Create user group members in Keycloak from JSON file")
    public void createGroupMembers(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processGroupMembersFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
                clientId != null ? clientId : keycloakConfig.getClientId(),
                clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }

    @ShellMethod("Create Perun groups and related configuration in Keycloak from JSON file")
    public void createPerunGroups(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processPerunGroupsFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
                clientId != null ? clientId : keycloakConfig.getClientId(),
                clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }

    @ShellMethod("Create Perun user group members in Keycloak from JSON file")
    public void createPerunMembers(
            @ShellOption (defaultValue = ShellOption.NULL, help = "Keycloak admin REST API") String keycloakUrl,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientId for getting token with clients credentials") String clientId,
            @ShellOption (defaultValue = ShellOption.NULL, help = "clientSecret for getting token with clients credentials") String clientSecret,
            @ShellOption (help = "COmanage json file") String jsonFilePath,
            @ShellOption (defaultValue = ShellOption.NULL, help = "comma seperated list of allowed parent groups") String allowedGroups) throws IOException {

        keycloakAdminService.processPerunGroupMembersFromFile(jsonFilePath, keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl(),
                clientId != null ? clientId : keycloakConfig.getClientId(),
                clientSecret != null ? clientSecret : keycloakConfig.getClientSecret(), allowedGroups == null ? new ArrayList<>() : Arrays.asList(allowedGroups.split(",")));
    }

    @ShellMethod("Create Perun top level user group members in Keycloak from JSON file")
    public void createPerunExtraMembers(
            @ShellOption (help = "COmanage json file") String jsonFilePath) throws IOException {

        keycloakAdminService.processPerunGroupMembersExtraFromFile(jsonFilePath, keycloakConfig.getUrl(), keycloakConfig.getClientId(), keycloakConfig.getClientSecret());
    }

    @ShellMethod("Delete all memberships of Perun top level group and its subgroups")
    public void deletePerunMembers(
            @ShellOption (defaultValue = "eiscat.se", help = "Perun VO") String perunVO) throws IOException {

        keycloakAdminService.processDeletePerunMembers(perunVO, keycloakConfig.getUrl(), keycloakConfig.getClientId(), keycloakConfig.getClientSecret());
    }
}

