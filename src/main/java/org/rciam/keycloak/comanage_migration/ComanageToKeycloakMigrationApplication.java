package org.rciam.keycloak.comanage_migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;

@SpringBootApplication(exclude = {ServletWebServerFactoryAutoConfiguration.class})
public class ComanageToKeycloakMigrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComanageToKeycloakMigrationApplication.class, args);
	}

}
