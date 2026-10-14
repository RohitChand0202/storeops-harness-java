package com.cognizant.storeops.staff.repository;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.domain.UserProfile;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savesAndFindsUsersAndTokens() {
        User user = new User("user-7", "a@b.com", "Lead", StaffRole.STORE_MANAGER,
                "store-12", "region-1", true);
        repository.save(user);
        repository.saveToken(new AuthToken("tok", "user-7", Instant.parse("2027-01-01T00:00:00Z")));

        assertThat(repository.findById("user-7")).contains(user);
        assertThat(repository.findById("nobody")).isEmpty();
        assertThat(repository.findToken("tok")).isPresent();
        assertThat(repository.findToken("tok").orElseThrow().userId()).isEqualTo("user-7");
        assertThat(repository.findToken("other")).isEmpty();
        assertThat(user.active()).isTrue();
        assertThat(user.name()).isEqualTo("Lead");
    }

    @Test
    void carriesProfileDetail() {
        UserProfile profile = new UserProfile("user-7", "Lead", "Grocery", "555");

        assertThat(profile.userId()).isEqualTo("user-7");
        assertThat(profile.displayName()).isEqualTo("Lead");
        assertThat(profile.department()).isEqualTo("Grocery");
        assertThat(profile.phone()).isEqualTo("555");
    }

    @Test
    void listsOnlyUsersInTheRequestedStore() {
        User inStore = new User("user-1", "one@example.com", "One", StaffRole.ASSOCIATE,
                "store-12", "region-1", true);
        User elsewhere = new User("user-2", "two@example.com", "Two", StaffRole.ASSOCIATE,
                "store-99", "region-2", true);
        repository.save(inStore);
        repository.save(elsewhere);

        assertThat(repository.findByStoreId("store-12")).containsExactly(inStore);
    }
}
