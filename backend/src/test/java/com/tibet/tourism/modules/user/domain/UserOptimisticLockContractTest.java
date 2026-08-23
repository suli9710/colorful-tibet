package com.tibet.tourism.modules.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class UserOptimisticLockContractTest {

    @Test
    void userWritesUseNonNullableOptimisticLockVersion() throws Exception {
        Field version = User.class.getDeclaredField("version");

        assertThat(version.getAnnotation(Version.class)).isNotNull();
        assertThat(version.getAnnotation(JsonIgnore.class)).isNotNull();
        assertThat(version.getAnnotation(Column.class)).isNotNull();
        assertThat(version.getAnnotation(Column.class).nullable()).isFalse();
    }
}
