package com.tibet.tourism.modules.spot.web;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;

class PriceControllerSecurityTest {

    @Test
    void fetchPriceRequiresAdminRoleBecauseItTriggersExternalPriceLookup() throws NoSuchMethodException {
        PreAuthorize annotation = PriceController.class
                .getMethod("fetchPrice", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
