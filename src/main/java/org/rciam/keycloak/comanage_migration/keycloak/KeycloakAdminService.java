package org.rciam.keycloak.comanage_migration.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageGroupRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserGroupMembership;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserRepresentation;
import org.rciam.keycloak.comanage_migration.common.ConvertFromComanageToKeycloak;
import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.rciam.keycloak.comanage_migration.dtos.GroupAupRepresentation;
import org.rciam.keycloak.comanage_migration.dtos.GroupEnrollmentConfigurationRepresentation;
import org.rciam.keycloak.comanage_migration.dtos.GroupsPager;
import org.rciam.keycloak.comanage_migration.dtos.PerunUserGroupMembershipRepresentation;
import org.rciam.keycloak.comanage_migration.dtos.PerunUsersMemberships;
import org.rciam.keycloak.comanage_migration.dtos.UserGroupMembershipExtensionRepresentation;
import org.rciam.keycloak.comanage_migration.dtos.UserGroupMembershipExtensionRepresentationPager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.rciam.keycloak.comanage_migration.common.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final org.rciam.keycloak.comanage_migration.keycloak.KeycloakTokenService tokenService;
    private final ConvertFromComanageToKeycloak converter;
    private final KeycloakConfig keycloakConfig;

    private static final String AUTHORIZATION = "Authorization";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String REALMS = "/realms";
    private static final String ADMIN_REALMS = "/admin/realms";
    private static final String USERS = "/users";
    private static final String GROUPS = "/groups";
    private static final String CHILDREN = "/children";
    private static final String GROUPS_ADMIN_URL = "/agm/account/group-admin/groups";
    private static final String GROUP_ADMIN_URL = "/agm/account/group-admin/group/";
    private static final String ROLES = "/roles";
    private static final String CONFIGURATION = "/configuration";
    private static final String MEMBERS = "/members";
    private static final String MEMBER = "/member/{memberId}";
    private static final String SUSPENSION = "/suspend";
    private static final String DEFAULT_CONFIGURATION = "/default-configuration";
    public static final String DEFAULT_CONFIGURATION_NAME = "defaultConfiguration";
    private static final String MEMBER_ROLE = "member";
    private static final String DESCRIPTION = "description";
    private static final String PERUN_AUP = "https://aai.egi.eu/aup/{0}.html";
    private static final List<String> EXCLUDE_GROUPS = Stream.of("vo.clarin.eu","vo.nbis.se","vo.nextgeoss.eu").toList();
    private static final List<String> memberRole= Stream.of("member").toList();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<String> excludedGroups = Stream.of("vo.nextgeoss.eu").toList();

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
            keycloakUrl = keycloakUrl.replace(REALMS, ADMIN_REALMS);
            // First check if user exists
            List<UserRepresentation> existingUsers = getUserByUsername(keycloakUrl, comanageUser.getUsername(), token);
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
                        .header(AUTHORIZATION, "Bearer " + token)
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .bodyValue(user);
            } else {
                // Create new user
                request = WebClient.builder()
                        .baseUrl(keycloakUrl + USERS)
                        .build()
                        .post()
                        .header(AUTHORIZATION, "Bearer " + token)
                        .header(CONTENT_TYPE, APPLICATION_JSON)
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

        comanageGroups.stream().filter(comanageGroup -> !excludedGroups.contains(comanageGroup.getName())).forEach(comanageGroup -> createGroupAndEnrollment(keycloakUrl, comanageGroup, token));


    }


    private void createGroupAndEnrollment(String keycloakUrl, ComanageGroupRepresentation comanageGroup, String token) {
        GroupsPager existingGroups = getGroupByName(keycloakUrl, comanageGroup.getName(), token);
        try {
            if (existingGroups.getCount() == 0) {
                    String groupId = createGroup(keycloakUrl, comanageGroup, token);

                    //create group role and default enrollment configuration

                    GroupEnrollmentConfigurationRepresentation configuration = comanageGroup.getEnrollmentConfigurationList() == null || comanageGroup.getEnrollmentConfigurationList().isEmpty() ? new GroupEnrollmentConfigurationRepresentation() : comanageGroup.getEnrollmentConfigurationList().get(0);
                    List<String> roles = new ArrayList<>();
                    if (comanageGroup.getEnrollmentConfigurationList() == null || comanageGroup.getEnrollmentConfigurationList().isEmpty()) {
                        //create default enrollment configuration
                        configuration.setName("Join " + comanageGroup.getName());
                        configuration.setRequireApproval(Boolean.TRUE);
                        configuration.setRequireApprovalForExtension(Boolean.TRUE);
                        configuration.setActive(Boolean.TRUE);
                        configuration.setVisibleToNotMembers(Boolean.FALSE);
                        configuration.setMultiselectRole(Boolean.TRUE);
                        roles.addAll(Stream.of(MEMBER_ROLE).toList());
                        configuration.setCommentsNeeded(Boolean.TRUE);
                        configuration.setCommentsLabel("Comments");
                        configuration.setCommentsDescription("Why do you want to join the group?");
                        configuration.setMembershipExpirationDays(comanageGroup.getParentName() != null ? null : Long.valueOf(365));
                    } else {
                        configuration.setMultiselectRole(Boolean.TRUE);
                        configuration.setActive(Boolean.TRUE);
                        configuration.setRequireApprovalForExtension(Boolean.TRUE);
                        roles.addAll(configuration.getGroupRoles());
                        if (configuration.getAup() != null) {
                            configuration.getAup().setType("URL");
                        }
                    }

                    if (comanageGroup.getParentName() == null) {
                        roles.add(Utils.DEFAULT_TOPLEVEL_ROLE);
                    }
                    configuration.setGroupRoles(roles);

                    roles.stream().forEach(role ->createGroupRole(keycloakUrl, groupId, role, token));

                    WebClient.builder()
                            .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupId + CONFIGURATION)
                            .build()
                            .post()
                            .header(AUTHORIZATION, "Bearer " + token)
                            .header(CONTENT_TYPE, APPLICATION_JSON)
                            .bodyValue(configuration).retrieve()
                            .toBodilessEntity()
                            .doOnSuccess(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    configuration.setId(StringUtils.substringAfter(response.getHeaders().getLocation().toString(), "configuration/"));
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

                    WebClient.builder()
                            .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupId + DEFAULT_CONFIGURATION)
                            .build()
                            .post()
                            .uri(uriBuilder -> uriBuilder
                                    .queryParam("configurationId", configuration.getId())
                                    .build())
                            .header(AUTHORIZATION, "Bearer " + token)
                            .header(CONTENT_TYPE, APPLICATION_JSON)
                            .retrieve()
                            .toBodilessEntity()
                            .doOnSuccess(response -> {
                                if (!response.getStatusCode().is2xxSuccessful()) {
                                    logger.error("Failed to create default group enrollment configuration for group {}: {}",
                                            comanageGroup.getName(),
                                            response.getBody());
                                    throw new RuntimeException("Problem adding default group enrollment configuration");
                                }
                            })
                            .doOnError(error -> {
                                logger.error("Failed to create default group enrollment configuration for group {}: {}",
                                        comanageGroup.getName(),
                                        error.getMessage());
                                throw new RuntimeException("Problem adding default group enrollment configuration");
                            })
                            .block();

                logger.info("Group with name {} together with all related entities has been created",
                        comanageGroup.getName());

            } else if (comanageGroup.getEnrollmentConfigurationList() != null && !comanageGroup.getEnrollmentConfigurationList().isEmpty()) {
                //try to default update enrollment configuration
                GroupRepresentation groupRep = existingGroups.getResults().get(0);

                String configurationId = groupRep.getAttributes().get(DEFAULT_CONFIGURATION_NAME).get(0);

                GroupEnrollmentConfigurationRepresentation configuration = WebClient.builder()
                        .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupRep.getId() + CONFIGURATION + "/" + configurationId)
                        .build()
                        .get()
                        .header(AUTHORIZATION, "Bearer " + token)
                        .retrieve().bodyToMono(GroupEnrollmentConfigurationRepresentation.class)
                        .block();

                if (configuration == null) {
                    throw new RuntimeException("Default configuration for this existing group does not exist");
                }

                GroupEnrollmentConfigurationRepresentation comanageConfiguration = comanageGroup.getEnrollmentConfigurationList().get(0);
                configuration.setName(comanageConfiguration.getName());
                configuration.setRequireApproval(comanageConfiguration.getRequireApproval());
                configuration.setVisibleToNotMembers(comanageConfiguration.isVisibleToNotMembers());
                configuration.setCommentsNeeded(comanageConfiguration.getCommentsNeeded());
                configuration.setCommentsLabel(comanageConfiguration.getCommentsLabel());
                configuration.setCommentsDescription(comanageConfiguration.getCommentsDescription());
                configuration.setMembershipExpirationDays(comanageConfiguration.getMembershipExpirationDays());
                List<String> roles = new ArrayList<>();
                roles.addAll(comanageConfiguration.getGroupRoles());
                if (comanageGroup.getParentName() == null) {
                    roles.add(Utils.DEFAULT_TOPLEVEL_ROLE);
                }
                configuration.setGroupRoles(roles);
                if (comanageConfiguration.getAup() != null){
                    if (configuration.getAup() == null)
                        configuration.setAup(new GroupAupRepresentation());
                    configuration.getAup().setType("URL");
                    configuration.getAup().setUrl(comanageConfiguration.getAup().getUrl());
                } else {
                    configuration.setAup(null);
                }

                WebClient.builder()
                        .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupRep.getId() + CONFIGURATION)
                        .build()
                        .post()
                        .header(AUTHORIZATION, "Bearer " + token)
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .bodyValue(configuration).retrieve()
                        .toBodilessEntity()
                        .doOnSuccess(response -> {
                            if (!response.getStatusCode().is2xxSuccessful()) {
                                logger.error("Failed to update group enrollment configuration with name {} for group {}: {}",
                                        configuration.getName(),
                                        comanageGroup.getName(),
                                        response.getBody());
                                throw new RuntimeException("Problem updating group enrollment configuration");
                            }
                        })
                        .doOnError(error -> {
                            logger.error("Failed to update group enrollment configuration with name {} for group {}: {}",
                                    configuration.getName(),
                                    comanageGroup.getName(),
                                    error.getMessage());
                            throw new RuntimeException("Problem updating group enrollment configuration");
                        })
                        .block();

                logger.info("Enrollment request with name {} for group with name {} together with all related entities has been updated",
                        comanageConfiguration.getName(), comanageGroup.getName());
            }
        } catch (Exception e) {
            logger.error("Error processing group {}: {}", comanageGroup.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

    private String createGroup(String keycloakUrl, ComanageGroupRepresentation comanageGroup, String token) {
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(comanageGroup.getName());
        String parentId = null;
        if (comanageGroup.getParentName() != null) {
            GroupsPager existingGroups = getGroupByName(keycloakUrl, comanageGroup.getParentName(), token);
            if (existingGroups.getCount() > 0) {
                parentId = existingGroups.getResults().get(0).getId();
            } else {
                logger.error("Group with name {} and parent group with name {} could not be created. Parent group does not exist.", comanageGroup.getName(), comanageGroup.getDescription());
                throw new RuntimeException("Parent group does not exist");
            }
        }
        if (comanageGroup.getDescription() != null) {
            groupRepresentation.setAttributes(Map.of(DESCRIPTION, Stream.of(comanageGroup.getDescription()).toList()));
        }

        String url = parentId == null ? keycloakUrl.replace(REALMS, ADMIN_REALMS) + GROUPS : keycloakUrl.replace(REALMS, ADMIN_REALMS) + GROUPS + "/" + parentId + CHILDREN;

        WebClient.builder()
                .baseUrl(url)
                .build()
                .post()
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .bodyValue(groupRepresentation).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Group with name {} has successfully created.",
                                groupRepresentation.getName());
                        groupRepresentation.setId(StringUtils.substringAfter(response.getHeaders().getLocation().toString(), "groups/"));
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
        return groupRepresentation.getId();
    }


    public void processPerunGroupsFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageGroupRepresentation> comanageGroups = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageGroupRepresentation.class));

        comanageGroups.stream().forEach(comanageGroup -> createPerunGroupAndEnrollment(keycloakUrl, comanageGroup, token));

    }

    private void createPerunGroupAndEnrollment(String keycloakUrl, ComanageGroupRepresentation comanageGroup, String token) {
        String[] groupsNameArray = comanageGroup.getName().split(":");
        if (EXCLUDE_GROUPS.contains(groupsNameArray[0])) {
            logger.warn("Perun group with name {} will not be created due to being comanage group",
            comanageGroup.getName());
            return;
        }
        GroupsPager parentGroups = getGroupByName(keycloakUrl, groupsNameArray[groupsNameArray.length - 2], token);
        if (parentGroups.getCount() > 1 || (parentGroups.getCount() == 0 && groupsNameArray.length > 2) ) {
            logger.warn("PERUN group with name {} can not be created because {}", comanageGroup.getName(),parentGroups.getCount()> 1 ? "parent group is not unique" : "no top level parent group does not exist");
            return;
        }
        try {
            String parentId = parentGroups.getCount() == 0 ? createPerunGroup(keycloakUrl, groupsNameArray[groupsNameArray.length - 2], null, null, token) : parentGroups.getResults().get(0).getId();
            createPerunGroup(keycloakUrl, groupsNameArray[groupsNameArray.length - 1], comanageGroup.getDescription(), parentId, token);
            logger.info("Perun group with name {} together with all related entities has been created",
                        comanageGroup.getName());

        } catch (Exception e) {
            logger.error("Error processing group {}: {}", comanageGroup.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

    private String createPerunGroup(String keycloakUrl, String name, String description, String parentId, String token) {
        boolean toplevel = parentId == null;
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(name);
        groupRepresentation.setAttributes(new HashMap<>());
        groupRepresentation.getAttributes().put("PERUN", Stream.of("true").toList());
        if (description != null) {
            groupRepresentation.getAttributes().put(DESCRIPTION, Stream.of(description).toList());
        }

        String url = toplevel ? keycloakUrl.replace(REALMS, ADMIN_REALMS) + GROUPS : keycloakUrl.replace(REALMS, ADMIN_REALMS) + GROUPS + "/" + parentId + CHILDREN;

        WebClient.builder()
                .baseUrl(url)
                .build()
                .post()
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .bodyValue(groupRepresentation).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Group with name {} has successfully created.",
                                groupRepresentation.getName());
                        groupRepresentation.setId(StringUtils.substringAfter(response.getHeaders().getLocation().toString(), "groups/"));
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

        //create group role and default enrollment configuration
        List<String> roles = Stream.of(MEMBER_ROLE).toList();
        createGroupRole(keycloakUrl, groupRepresentation.getId(), MEMBER_ROLE, token);
        GroupEnrollmentConfigurationRepresentation configuration = new GroupEnrollmentConfigurationRepresentation();
        configuration.setName("Join " + name);
        configuration.setRequireApproval(Boolean.TRUE);
        configuration.setRequireApprovalForExtension(Boolean.TRUE);
        configuration.setActive(Boolean.TRUE);
        configuration.setVisibleToNotMembers(Boolean.FALSE);
        configuration.setMultiselectRole(Boolean.FALSE);
        configuration.setGroupRoles(roles);
        configuration.setCommentsNeeded(Boolean.FALSE);
        if (toplevel) {
            configuration.setMembershipExpirationDays(Long.valueOf(365));
            GroupAupRepresentation aup = new GroupAupRepresentation();
            aup.setType("URL");
            aup.setUrl(PERUN_AUP.replace("{0}", name));
            configuration.setAup(aup);
        }
        configuration.setGroupRoles(roles);

        WebClient.builder()
                .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupRepresentation.getId() + CONFIGURATION)
                .build()
                .post()
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .bodyValue(configuration).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        configuration.setId(StringUtils.substringAfter(response.getHeaders().getLocation().toString(), "configuration/"));
                    } else {
                        logger.error("Failed to create group enrollment configuration with name {} for group {}: {}",
                                configuration.getName(),
                                name,
                                response.getBody());
                        throw new RuntimeException("Problem creating group enrollment configuration");
                    }
                })
                .doOnError(error -> {
                    logger.error("Failed to create group enrollment configuration with name {} for group {}: {}",
                            configuration.getName(),
                            name,
                            error.getMessage());
                    throw new RuntimeException("Problem creating group enrollment configuration");
                })
                .block();

        WebClient.builder()
                .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupRepresentation.getId() + DEFAULT_CONFIGURATION)
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("configurationId", configuration.getId())
                        .build())
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        logger.error("Failed to create default group enrollment configuration for group {}: {}",
                                name,
                                response.getBody());
                        throw new RuntimeException("Problem adding default group enrollment configuration");
                    }
                })
                .doOnError(error -> {
                    logger.error("Failed to create default group enrollment configuration for group {}: {}",
                            name,
                            error.getMessage());
                    throw new RuntimeException("Problem adding default group enrollment configuration");
                })
                .block();

        return groupRepresentation.getId();
    }


    public void processGroupAdminsFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageUserGroupMembership> groupAdmins = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageUserGroupMembership.class));

        groupAdmins.stream().filter(groupAdmin -> !excludedGroups.contains(groupAdmin.getGroupName())).forEach(groupAdmin -> createGroupAdmin(keycloakUrl, groupAdmin.getUsername(), groupAdmin.getGroupName(), token));
    }

    private void createGroupAdmin(String keycloakUrl, String username, String groupName, String token) {

        try {
            List<UserRepresentation> users = getUserByUsername(keycloakUrl.replace(REALMS, ADMIN_REALMS), username, token);

            if (users == null || users.isEmpty()) {
                throw new RuntimeException(String.format("User with username %s does not exist.", username));
            }

            // Retrieve group by group name with brief representation
            GroupsPager groups = getGroupByName(keycloakUrl, groupName, token);
            if (groups.getCount() == 0) {
                throw new RuntimeException(String.format("Group with name %s does not exist.", groupName));
            }

            // Create group admin
            WebClient.builder()
                    .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groups.getResults().get(0).getId() + "/admin")
                    .build()
                    .post()
                    .uri(uriBuilder -> uriBuilder.queryParam("username", username).build())
                    .header(AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(response -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            logger.info("Group admin for user {} in group {} has been created",
                                    username, groupName);
                        } else {
                            logger.warn("Failed to create group admin for user {} in group {}: {}",
                                    username, groupName, response.getBody());
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Failed to create group admin for user {} in group {}: {}",
                                username, groupName, error.getMessage());
                        throw new RuntimeException("Problem creating group admin");
                    })
                    .block();

        } catch (Exception e) {
            logger.error("Failed to create group admin for user {} in group {}: {}",
                    username, groupName, e.getMessage());
            e.printStackTrace();
        }
    }

    public void processGroupMembersFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        List<ComanageUserGroupMembership> groupAdmins = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ComanageUserGroupMembership.class));

        groupAdmins.stream().filter(comanageMember -> !excludedGroups.contains(comanageMember.getGroupName())).forEach(comanageMember -> createGroupMembers(keycloakUrl, comanageMember, token));
    }

    private void createGroupMembers(String keycloakUrl, ComanageUserGroupMembership comanageMember, String token) {

        try {
            List<UserRepresentation> users = getUserByUsername(keycloakUrl.replace(REALMS, ADMIN_REALMS), comanageMember.getUsername(), token);

            if (users == null || users.isEmpty()) {
                throw new RuntimeException(String.format("User with username %s for the group %s does not exist.", comanageMember.getUsername(), comanageMember.getGroupName()));
            }

            // Retrieve group by group name with brief representation
            GroupsPager groups = getGroupByName(keycloakUrl, comanageMember.getGroupName(), token);
            if (groups.getCount() == 0) {
                throw new RuntimeException(String.format("Group with name %s does not exist.", comanageMember.getGroupName()));
            }
            GroupRepresentation group = groups.getResults().get(0);

            List<String> existingRoles = WebClient.builder()
                    .baseUrl(keycloakUrl + GROUP_ADMIN_URL + group.getId() + ROLES)
                    .build()
                    .get()
                    .header(AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block();
            List<String> newRoles = new ArrayList<>(); // for adding possible new roles

            UserGroupMembershipExtensionRepresentationPager memberPager = WebClient.builder()
                    .baseUrl(keycloakUrl + GROUP_ADMIN_URL + group.getId() + MEMBERS)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("search", comanageMember.getUsername())
                            .build())
                    .header(AUTHORIZATION, "Bearer " + token)
                    .retrieve().bodyToMono(UserGroupMembershipExtensionRepresentationPager.class)
                    .block();

            UserGroupMembershipExtensionRepresentation member = converter.convertMember(comanageMember, group.getPath().split("/").length == 2, newRoles, existingRoles);
            newRoles.stream().forEach(role -> createGroupRole(keycloakUrl, group.getId(), role, token));
            if (memberPager.getCount() == 0) {

                createMember(keycloakUrl, token, group.getName(), comanageMember.getUsername(), group.getId(), member);

                if ("SUSPENDED".equals(comanageMember.getStatus())) {
                    suspendMember(keycloakUrl, token, group, comanageMember.getUsername(), member.getId());
                }
            } else {

                UserGroupMembershipExtensionRepresentation oldMember = memberPager.getResults().get(0);
                boolean isChanged = false;
                if (member.getValidFrom().isBefore(oldMember.getValidFrom())){
                    oldMember.setValidFrom(member.getValidFrom());
                    isChanged = true;
                }
                if (oldMember.getMembershipExpiresAt() != null && ( member.getMembershipExpiresAt() == null || ( member.getMembershipExpiresAt()!= null && member.getMembershipExpiresAt().isAfter(oldMember.getMembershipExpiresAt())))){
                    oldMember.setMembershipExpiresAt(member.getMembershipExpiresAt());
                    isChanged = true;
                }
                if (!oldMember.getGroupRoles().equals(member.getGroupRoles())) {
                    oldMember.setGroupRoles(member.getGroupRoles());
                    isChanged = true;
                }
                if (isChanged){
                    WebClient.builder()
                            .baseUrl(keycloakUrl + GROUP_ADMIN_URL + group.getId() +  MEMBER.replace("{memberId}", oldMember.getId()))
                            .build()
                            .put()
                            .header(AUTHORIZATION, "Bearer " + token)
                            .header(CONTENT_TYPE, APPLICATION_JSON)
                            .bodyValue(oldMember).retrieve()
                            .toBodilessEntity()
                            .doOnSuccess(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    logger.info("User group membership of the group {} and user {} has been updated successfully",
                                            group.getName(),
                                            comanageMember.getUsername());
                                } else {
                                    logger.error("Failed to update user group membership of the group {} and user {}: {}",
                                            group.getName(),
                                            comanageMember.getUsername(),
                                            response.getBody());
                                    throw new RuntimeException("Problem creating group membership");
                                }
                            })
                            .doOnError(error -> {
                                logger.error("Failed to update user group membership of the group {} and user {}: {}",
                                        group.getName(),
                                        comanageMember.getUsername(),
                                        error.getMessage());
                                throw new RuntimeException("Problem creating group membership");
                            })
                            .block();
                }

                if ("SUSPENDED".equals(comanageMember.getStatus()) && !"SUSPENDED".equals(oldMember.getStatus())) {
                    suspendMember(keycloakUrl, token, group, comanageMember.getUsername(), oldMember.getId());
                }
            }


        } catch (Exception e) {
            logger.error("Failed to create user group membership for the group {} and user {}: {}",
                    comanageMember.getGroupName(),
                    comanageMember.getUsername(), e.getMessage());
            e.printStackTrace();
        }

    }

    public void processPerunGroupMembersFromFile(String jsonFilePath, String keycloakUrl, String clientId, String clientSecret) throws IOException {
        String token = tokenService.getToken(keycloakUrl, clientId, clientSecret);

        Map<String, PerunUserGroupMembershipRepresentation>  perunMembers = objectMapper.readValue(Files.readAllBytes(Path.of(jsonFilePath)), PerunUsersMemberships.class).getUsers();
        perunMembers.entrySet().stream()
                .filter(entry -> !entry.getValue().getGroups().isEmpty())
                .forEach(entry -> createPerunGroupMembers(keycloakUrl, entry.getKey(), entry.getValue().getGroups(), token));
    }

    private void createPerunGroupMembers(String keycloakUrl, String username, List<String> groups, String token) {

        List<UserRepresentation> users = getUserByUsername(keycloakUrl.replace(REALMS, ADMIN_REALMS), username, token);

        if (users == null || users.isEmpty()) {
            logger.error("User with username {} for the groups {} does not exist.", username, groups.stream().collect(Collectors.joining(",")));
            return;
        }

        groups.stream().forEach(groupPath -> {
            String[] groupsNameArray = groupPath.split(":");
            if (!EXCLUDE_GROUPS.contains(groupsNameArray[0])) {
                try {
                    String groupPathKeycloak = "/" + groupPath.replace(":","/");
                    GroupsPager groupPager = getGroupByName(keycloakUrl, groupsNameArray[groupsNameArray.length - 1], token);
                    Optional<GroupRepresentation> group = groupPager.getResults().stream().filter(gr -> groupPathKeycloak.equals(gr.getPath())).findAny();
                    if (group.isEmpty()) {
                        logger.warn(String.format("Group with path %s does not exist.", groupPath));
                        return;
                    }

                    UserGroupMembershipExtensionRepresentationPager memberPager = WebClient.builder()
                            .baseUrl(keycloakUrl + GROUP_ADMIN_URL + group.get().getId() + MEMBERS)
                            .build()
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .queryParam("search", username)
                                    .build())
                            .header(AUTHORIZATION, "Bearer " + token)
                            .retrieve().bodyToMono(UserGroupMembershipExtensionRepresentationPager.class)
                            .block();

                   if (memberPager.getCount() == 0) {

                       if ("members".equals(group.get().getName())){
                           UserGroupMembershipExtensionRepresentation memberParent = new UserGroupMembershipExtensionRepresentation();
                           GroupsPager parentGroupPager = getGroupByName(keycloakUrl, groupsNameArray[groupsNameArray.length - 2], token);
                           UserGroupMembershipExtensionRepresentationPager memberParentPager = WebClient.builder()
                                   .baseUrl(keycloakUrl + GROUP_ADMIN_URL + parentGroupPager.getResults().get(0).getId() + MEMBERS)
                                   .build()
                                   .get()
                                   .uri(uriBuilder -> uriBuilder
                                           .queryParam("search", username)
                                           .build())
                                   .header(AUTHORIZATION, "Bearer " + token)
                                   .retrieve().bodyToMono(UserGroupMembershipExtensionRepresentationPager.class)
                                   .block();

                           if (memberParentPager.getCount()  == 0) {
                               GroupRepresentation parentGroup = parentGroupPager.getResults().get(0);
                               memberParent.setUser(users.get(0));
                               memberParent.setGroupRoles(memberRole);
                               memberParent.setMembershipExpiresAt(LocalDate.now().plusYears(1));

                               createMember(keycloakUrl, token, parentGroup.getPath(), username, group.get().getId(), memberParent);
                           }
                       }

                       UserGroupMembershipExtensionRepresentation member = new UserGroupMembershipExtensionRepresentation();
                       member.setUser(users.get(0));
                       member.setGroupRoles(memberRole);
                       createMember(keycloakUrl, token, groupPath, username, group.get().getId(), member);
                    } else {
                       logger.info("User group membership of the group {} and user {} exists",
                               groupPath,
                               username);
                   }

                } catch (Exception e) {
                    logger.error("Failed to create user group membership for the group {} and user {}: {}",
                            groupPath,
                            username, e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void createMember(String keycloakUrl, String token, String groupPath, String username, String groupId, UserGroupMembershipExtensionRepresentation member){
        WebClient.builder()
                .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupId + MEMBERS)
                .build()
                .post()
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .bodyValue(member).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("User group membership of the group {} and user {} has been created successfully",
                                groupPath,
                                username);
                        member.setId(StringUtils.substringAfter(response.getHeaders().getLocation().toString(), "member/"));
                    } else {
                        logger.error("Failed to create user group membership of the group {} and user {}: {}",
                                groupPath,
                                username,
                                response.getBody());
                        throw new RuntimeException("Problem creating group membership");
                    }
                })
                .doOnError(error -> {
                    error.printStackTrace();
                    logger.error("Failed to create user group membership of the group {} and user {}: {}",
                            groupPath,
                            username,
                            error.getMessage());
                    throw new RuntimeException("Problem creating group membership");
                })
                .block();
    }

    private GroupsPager getGroupByName(String keycloakUrl, String name, String token) {
        return WebClient.builder()
                .baseUrl(keycloakUrl+ GROUPS_ADMIN_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("search", name)
                        .queryParam("toplevel", false)
                        .queryParam("exact", true)
                        .queryParam("max", 50)
                        .build())
                .header(AUTHORIZATION, "Bearer " + token)
                .retrieve().bodyToMono(GroupsPager.class)
                .block();
    }

    private List<UserRepresentation> getUserByUsername(String keycloakUrl, String username, String token) {
        return WebClient.builder()
                .baseUrl(keycloakUrl + USERS)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build())
                .header(AUTHORIZATION, "Bearer " + token)
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<UserRepresentation>>() {
                })
                .block();
    }

    private void createGroupRole(String keycloakUrl, String groupId, String role, String token){
        WebClient.builder()
                .baseUrl(keycloakUrl + GROUP_ADMIN_URL + groupId + ROLES)
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("name", role)
                        .build())
                .header(AUTHORIZATION, "Bearer " + token)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .retrieve()
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
    }

    private void suspendMember(String keycloakUrl, String token, GroupRepresentation group, String username, String memberId){
        WebClient.builder()
                .baseUrl(keycloakUrl + GROUP_ADMIN_URL +  group.getId() + MEMBER.replace("{memberId}", memberId) + SUSPENSION)
                .build()
                .post()
                .header(AUTHORIZATION, "Bearer " + token).retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        logger.error("Failed to suspend user {} in the group {}: {}",
                                username,
                                group.getName(),
                                response.getBody());
                        throw new RuntimeException("Problem suspending group member");
                    }
                })
                .doOnError(error -> {
                    logger.error("Failed to suspend user {} in the group {}: {}",
                            username,
                            group.getName(),
                            error.getMessage());
                    throw new RuntimeException("Problem suspending group member");
                })
                .block();
    }
}
