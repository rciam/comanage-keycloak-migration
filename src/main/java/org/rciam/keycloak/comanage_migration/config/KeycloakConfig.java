package org.rciam.keycloak.comanage_migration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {

    private String url ;
    private String clientId;
    private String clientSecret;
    private String egiCheckInUrl;
    private List<String> excludedAlias;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getEgiCheckInUrl() {
        return egiCheckInUrl;
    }

    public void setEgiCheckInUrl(String egiCheckInUrl) {
        this.egiCheckInUrl = egiCheckInUrl;
    }

    public List<String> getExcludedAlias() {
        return excludedAlias;
    }

    public void setExcludedAlias(List<String> excludedAlias) {
        this.excludedAlias = excludedAlias;
    }
}