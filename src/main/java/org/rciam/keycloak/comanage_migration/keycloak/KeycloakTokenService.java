package org.rciam.keycloak.comanage_migration.keycloak;

import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Component
public class KeycloakTokenService {

    private static final String TOKEN_URL = "/protocol/openid-connect/token";

    public String getToken(String keycloakUrl, String clientId, String clientSecret) {
        return WebClient.builder().baseUrl(keycloakUrl + TOKEN_URL).build().post()
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Keycloak token request failed with status " 
                                        + response.statusCode() + ": " + body)))
                .bodyToMono(AccessTokenResponse.class)
                .block().getToken();
    }
}
