package domaindriven.splearn.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProfileTest {
    @Test
    void profile() {
        new Profile("byunguk");
        new Profile("12345");
        new Profile("byunguk123");
    }

    @Test
    void profileFail() {
        assertThatThrownBy(() -> new Profile("byunguk!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Profile("byunguk!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Profile("병욱")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Profile("veryveryverylonglonglongaddress")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void url() {
        var profile = new Profile("byunguk");

        assertThat(profile.url()).isEqualTo("@byunguk");
    }
}
