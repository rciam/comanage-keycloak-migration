package org.rciam.keycloak.comanage_migration.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageGroupRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserRepresentation;
import org.rciam.keycloak.comanage_migration.common.ConvertFromComanageToKeycloak;
import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.rciam.keycloak.comanage_migration.dtos.GroupEnrollmentConfigurationRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Component
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final org.rciam.keycloak.comanage_migration.keycloak.KeycloakTokenService tokenService;
    private final ConvertFromComanageToKeycloak converter;
    private final KeycloakConfig keycloakConfig;

    private static final String USERS = "/users";
    private static final String GROUPS = "/groups";
    private static final String CHILDREN = "/children";
    private static final String AGM_ADMIN_GROUP = "/agm/admin/group/";
    private static final String GROUP_ADMIN_URL = "/account/group-admin/group/";
    private static final String ROLES = "/roles";
    private static final String CONFIGURATION = "/configuration";
    private static final String DEFAULT_TOPLEVEL_ROLE = "vm_operator";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public KeycloakAdminService(org.rciam.keycloak.comanage_migration.keycloak.KeycloakTokenService tokenService, ConvertFromComanageToKeycloak converter, KeycloakConfig keycloakConfig) {
        this.tokenService = tokenService;
        this.converter = converter;
        this.keycloakConfig = keycloakConfig;
    }

    public void processUsersFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageUserRepresentation> comanageUsers = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageUserRepresentation.class));

        comanageUsers.stream()
                .filter(user -> ( user.getFederatedIdentities() == null ||
                        user.getFederatedIdentities().stream()
                                .noneMatch(identity -> keycloakConfig.getExcludedAlias().contains(identity.getIdentityProvider())) && user.getTerms_and_conditions() != null)
                ).forEach(comanangeUser -> createUpdateUser(keycloakUrl, comanangeUser, token));

    }

    private void createUpdateUser(String keycloakUrl, ComanageUserRepresentation comanageUser, String token) {
        try {
            keycloakUrl = keycloakUrl.replace("/realms", "/admin/realms");
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
                    .retrieve().bodyToMono(new ParameterizedTypeReference<List<UserRepresentation>>() {
                    })
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
                        } else {
                            logger.error("Failed to {} user {}: {}",
                                    user.getId() != null ? "update" : "create",
                                    comanageUser.getUsername(),
                                    response.getBody());
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

    public void processGroupsFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageGroupRepresentation> comanageGroups = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageGroupRepresentation.class));

        comanageGroups.stream().forEach(comanageGroup -> createGroupAndEnrollment(keycloakUrl, comanageGroup, token));


    }

    private void createGroupAndEnrollment(String keycloakUrl, ComanageGroupRepresentation comanageGroup, String token) {
        List<GroupRepresentation> existingGroups = getGroupByName(keycloakUrl, comanageGroup.getName(), token);
        if (existingGroups.isEmpty()) {
            try {
                String groupId = createGroup(keycloakUrl, comanageGroup, token);
                if (comanageGroup.getEnrollmentConfigurationList() != null) {
                    //create group role and default enrollment configuration
                    List<String> roles = comanageGroup.getEnrollmentConfigurationList().get(0).getGroupRoles();
                    if (comanageGroup.getParentName() == null) {
                        roles.add(DEFAULT_TOPLEVEL_ROLE);
                        comanageGroup.getEnrollmentConfigurationList().get(0).getGroupRoles().add(DEFAULT_TOPLEVEL_ROLE);
                    }
                    roles.stream().forEach(role -> {
                        WebClient.builder()
                                .baseUrl(keycloakUrl + AGM_ADMIN_GROUP + groupId + ROLES)
                                .build()
                                .post()
                                .header("Authorization", "Bearer " + token)
                                .header("Content-Type", "application/json")
                                .bodyValue(role).retrieve()
                                .toBodilessEntity()
                                .doOnSuccess(response -> {
                                    if (!response.getStatusCode().is2xxSuccessful()) {
                                        logger.error("Failed to create group role with name {}: {}",
                                                role,
                                                response.getBody());
                                        throw new RuntimeException("Problem creating role");
                                    }
                                })
                                .doOnError(error -> {
                                    logger.error("Failed to create group role with name {}: {}",
                                            role,
                                            error.getMessage());
                                    throw new RuntimeException("Problem creating role");
                                })
                                .block();
                    });
                    GroupEnrollmentConfigurationRepresentation configuration = comanageGroup.getEnrollmentConfigurationList().get(0);
                    configuration.setMultiselectRole(Boolean.TRUE);
                    configuration.setActive(Boolean.TRUE);
                    if (configuration.getAup() != null){
                        configuration.getAup().setType("URL");
                    }

                    WebClient.builder()
                            .baseUrl(keycloakUrl + AGM_ADMIN_GROUP + groupId + CONFIGURATION)
                            .build()
                            .post()
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .bodyValue(configuration).retrieve()
                            .toBodilessEntity()
                            .doOnSuccess(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                  //  configuration.setId();
                                } else {
                                    logger.error("Failed to create group enrollment configuration with name {} for group {}: {}",
                                            configuration.getName(),
                                            comanageGroup.getName(),
                                            response.getBody());
                                    throw new RuntimeException("Problem creating group enrollment configuration");
                                }
                            })
                            .doOnError(error -> {
                                logger.error("Failed to create group enrollment configuration with name {} for group {}: {}",
                                        configuration.getName(),
                                        comanageGroup.getName(),
                                        error.getMessage());
                                throw new RuntimeException("Problem creating group enrollment configuration");
                            })
                            .block();

                } else {
                    // group enrollment configuration does not exist => default has been
                }
            } catch (RuntimeException e) {

            }
        }

    }

    private String createGroup(String keycloakUrl, ComanageGroupRepresentation comanageGroup, String token) {
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(comanageGroup.getName());
        String parentId = null;
        if (comanageGroup.getParentName() != null) {
            List<GroupRepresentation> existingGroups = getGroupByName(keycloakUrl, comanageGroup.getParentName(), token);
            if (!existingGroups.isEmpty()) {
                parentId = existingGroups.get(0).getId();
            } else {
                logger.error("Group with name {} and parent group with name {} could not be created. Parent group does not exist.", comanageGroup.getName(), comanageGroup.getDescription());
                throw new RuntimeException("Parent group does not exist");
            }
        }
        if (comanageGroup.getDescription() != null) {
            groupRepresentation.setAttributes(Map.of("description", Stream.of(comanageGroup.getDescription()).toList()));
        }

        String url = null;
        if (comanageGroup.getEnrollmentConfigurationList() != null) {
            url = parentId == null ? keycloakUrl.replace("/realms", "/admin/realms") + GROUPS : keycloakUrl.replace("/realms", "/admin/realms") + GROUPS + "/" + parentId + CHILDREN;
        } else {
            url = parentId == null ? keycloakUrl + AGM_ADMIN_GROUP : keycloakUrl + AGM_ADMIN_GROUP + parentId + CHILDREN;
        }

        AtomicReference<String> groupId = null;
        WebClient.builder()
                .baseUrl(url)
                .build()
                .post()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .bodyValue(groupRepresentation).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Group with name {} has successfully created.",
                                groupRepresentation.getName());
                        groupId.set(StringUtils.substringAfter(response.getHeaders().getFirst("Location"), "/groups/"));
                    } else {
                        logger.error("Failed to create group with name {}: {}",
                                groupRepresentation.getName(),
                                response.getBody());
                        throw new RuntimeException("Problem creating group");
                    }
                })
                .doOnError(error -> {
                    logger.error("Failed to create group with name {}: {}",
                            groupRepresentation.getName(),
                            error.getMessage());
                    throw new RuntimeException("Problem creating group");
                })
                .block();
        return groupId.get();
    }

    private List<GroupRepresentation> getGroupByName(String keycloakUrl, String name, String token) {
        return WebClient.builder()
                .baseUrl(keycloakUrl.replace("/realms", "/admin/realms") + GROUPS)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("search", name)
                        .queryParam("exact", true)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<GroupRepresentation>>() {
                })
                .block();
    }
}
