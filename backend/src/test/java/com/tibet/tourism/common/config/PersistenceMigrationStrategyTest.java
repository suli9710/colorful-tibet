package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class PersistenceMigrationStrategyTest {

    private static final String DDL_AUTO = "spring.jpa.hibernate.ddl-auto";
    private static final String FLYWAY_ENABLED = "spring.flyway.enabled";

    @Test
    void localProfileLetsFlywayOwnSchemaChanges() {
        assertProfileStrategy("application-local.yml");
    }

    @Test
    void testProfileLetsFlywayOwnSchemaChangesWithoutCreatingSchema() {
        assertProfileStrategy("application-test.yml");
    }

    @Test
    void prodProfileKeepsSchemaValidationOnly() {
        assertProfileStrategy("application-prod.yml");
    }

    private void assertProfileStrategy(String profileResource) {
        ConfigurableEnvironment environment = effectiveEnvironment(profileResource);

        assertThat(environment.getProperty(FLYWAY_ENABLED, Boolean.class)).isTrue();
        assertThat(environment.getProperty(DDL_AUTO)).isIn("validate", "none");
        assertThat(environment.getProperty(DDL_AUTO)).isNotIn("update", "create", "create-drop");
    }

    private ConfigurableEnvironment effectiveEnvironment(String profileResource) {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        addYaml(propertySources, "application.yml", false);
        addYaml(propertySources, profileResource, true);

        return environment;
    }

    private void addYaml(MutablePropertySources propertySources, String resourceName, boolean highestPrecedence) {
        ClassPathResource resource = new ClassPathResource(resourceName);
        assertThat(resource.exists()).as("%s exists on the test classpath", resourceName).isTrue();

        List<PropertySource<?>> yamlSources;
        try {
            yamlSources = new YamlPropertySourceLoader().load(resourceName, resource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + resourceName, exception);
        }

        for (PropertySource<?> yamlSource : yamlSources) {
            if (highestPrecedence) {
                propertySources.addFirst(yamlSource);
            } else {
                propertySources.addLast(yamlSource);
            }
        }
    }
}
