package org.rciam.keycloak.comanage_migration.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserRepresentation;
import org.rciam.keycloak.comanage_migration.common.ConvertFromComanageToKeycloak;
import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final org.rciam.keycloak.comanage_migration.keycloak.KeycloakTokenService tokenService;
    private final ConvertFromComanageToKeycloak converter;
    private final KeycloakConfig keycloakConfig;

    private static final String USERS = "/users";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public KeycloakAdminService(org.rciam.keycloak.comanage_migration.keycloak.KeycloakTokenService tokenService, ConvertFromComanageToKeycloak converter, KeycloakConfig keycloakConfig) {
        this.tokenService = tokenService;
        this.converter = converter;
        this.keycloakConfig = keycloakConfig;
    }

    public void processUsersFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageUserRepresentation> comanangeUsers = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageUserRepresentation.class));

        comanangeUsers.stream()
                .filter(user -> user.getFederatedIdentities() == null ||
                        user.getFederatedIdentities().stream()
                                .noneMatch(identity -> keycloakConfig.getExcludedAlias().contains(identity.getIdentityProvider()))
                ).forEach(comanangeUser -> createUpdateUser(keycloakUrl, comanangeUser, token));

    }

    private void createUpdateUser(String keycloakUrl, ComanageUserRepresentation comanageUser, String token) {
        try {
            // First check if user exists
            List<UserRepresentation> existingUsers = WebClient.builder()
                    .baseUrl(keycloakUrl + USERS)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("username", comanageUser.getUsername())
                            .queryParam("exact", true)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve().bodyToMono(new ParameterizedTypeReference<List<UserRepresentation>>() {})
                    .block();

            UserRepresentation user = existingUsers != null && !existingUsers.isEmpty() ? existingUsers.get(0) : new UserRepresentation();
            converter.convertUser(comanageUser, user);
            
            // Create or update user based on whether they existed
            WebClient.RequestHeadersSpec<?> request;
            if (user.getId() != null) {
                // Update existing user
                request = WebClient.builder()
                        .baseUrl(keycloakUrl + USERS + "/" + user.getId())
                        .build()
                        .put()
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .bodyValue(user);
            } else {
                // Create new user
                user.setCreatedTimestamp(comanageUser.getTerms_and_conditions().toInstant(ZoneOffset.UTC).toEpochMilli());
                request = WebClient.builder()
                        .baseUrl(keycloakUrl + USERS)
                        .build()
                        .post()
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .bodyValue(user);
            }

            request.retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(response -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            logger.info("User with username {} has been {}",
                                    comanageUser.getUsername(),
                                user.getId() != null ? "updated" : "created");
                        }
                    })
                    .doOnError(error -> logger.error("Failed to {} user {}: {}",
                        user.getId() != null ? "update" : "create",
                            comanageUser.getUsername(),
                        error.getMessage()))
                    .block();
        } catch (Exception e) {
            logger.error("Error processing user {}: {}", comanageUser.getUsername(), e.getMessage());
            e.printStackTrace();
        }
    }
}
