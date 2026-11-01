package domaindriven.splearn.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void equality() {
        var email1 = new Email("byunguk@splearn.com");
        var email2 = new Email("byunguk@splearn.com");

        assertThat(email1).isEqualTo(email2);
    }
}