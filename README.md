# keycloak-comanage-migration

## General description
This is a Spring Boot Shell project that is used to execute all needed scripts in order to migrate COmanage users, groups and related entitiesfrom JSON files to Keycloak.

## How to use it
To run the projectuse the following command: mvn spring-boot:run

## General configuration options 

Before using it, you need to create a service account client in Keycloak. Ensure that this client has sufficient access token expiration (e.g.1 hour) and the realm-admin role. This is necessary because each command firstly retrieves an access token and executes all required interactions with the Keycloak admin REST API using this token.

### Configuration options from application.yml

You must change these parameters before running script. 
Or alternatively project can be configured having different profiles.

- **keycloak.url**: The URL for the Keycloak admin REST API.
- **keycloak.client-id**: The Client ID used for obtaining tokens with client credentials.
- **keycloak.client-secret**: The Client Secret used for obtaining tokens with client credentials.
- **keycloak.egi-check-in-url**: The URL for the EGI check-in service(Comanage). Default is `https://aai-dev.egi.eu/`.
- **keycloak.excluded-alias**: A list of Comanange IdP aliases that are excluded from the migration process. 
- **logging.file.name**: The path to the log file where application logs will be stored. 

## Script commands
- **help**: `{x}` : Without `x`, all possible commands are shown. With `x`, it shows all help information for the specificcommand.
- **createUsers**: This command allows you to migrate Comanage users to Keycloak users from a specified JSON file. 
  - **Keycloak admin URL**: Keycloak admin REST API. Default value from `keycloak.url` of `application.yml`.
  - **Client ID**: ClientId for getting token with clients credentials. Default value from `keycloak.client-id` of`application.yml`.
  - **Client Secret**: ClientSecret for getting token with clients credentials. Default value from `keycloak.client-secret` of `application.yml`.
  - **Path to JSON file**: COmanage json file. Required.



