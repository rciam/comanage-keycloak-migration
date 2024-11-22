package org.rciam.keycloak.comanage_migration;

import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.rciam.keycloak.comanage_migration.keycloak.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

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

        String effectiveKeycloakUrl = keycloakUrl != null ? keycloakUrl : keycloakConfig.getUrl();

        keycloakAdminService.processUsersFromFile(jsonFilePath, effectiveKeycloakUrl, 
            clientId != null ? clientId : keycloakConfig.getClientId(), 
            clientSecret != null ? clientSecret : keycloakConfig.getClientSecret());
    }
}

