package com.cognizant.storeops.shared.domain;

import java.time.Clock;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.StoreOpsApplication;

import static org.assertj.core.api.Assertions.assertThat;

class StoreOpsUserTest {

    @Test
    void carriesTheScopeEveryEndpointApplies() {
        StoreOpsUser user = new StoreOpsUser("user-7", "store-12", "region-1", "DEPARTMENT_LEAD");

        assertThat(user.id()).isEqualTo("user-7");
        assertThat(user.storeId()).isEqualTo("store-12");
        assertThat(user.regionId()).isEqualTo("region-1");
        assertThat(user.role()).isEqualTo("DEPARTMENT_LEAD");
    }

    @Test
    void theApplicationExposesAnInjectableClock() {
        Clock clock = new StoreOpsApplication().clock();

        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
    }
}
