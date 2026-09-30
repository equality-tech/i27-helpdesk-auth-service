package com.i27.helpdesk.auth.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void lifecycleHooksSetCreationAndUpdateTimestamps() {
        User user = new User();

        user.onCreate();

        LocalDateTime createdAt = (LocalDateTime) ReflectionTestUtils.getField(user, "createdAt");
        LocalDateTime firstUpdatedAt = (LocalDateTime) ReflectionTestUtils.getField(user, "updatedAt");
        assertThat(createdAt).isNotNull();
        assertThat(firstUpdatedAt).isNotNull();

        user.onUpdate();

        LocalDateTime updatedAt = (LocalDateTime) ReflectionTestUtils.getField(user, "updatedAt");
        assertThat(updatedAt).isAfterOrEqualTo(firstUpdatedAt);
    }
}
