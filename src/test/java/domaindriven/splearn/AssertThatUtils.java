package domaindriven.splearn;

import static org.assertj.core.api.Assertions.assertThat;

import domaindriven.splearn.domain.member.MemberRegisterRequest;
import java.util.function.Consumer;
import org.assertj.core.api.AssertProvider;
import org.springframework.test.json.JsonPathValueAssert;

public class AssertThatUtils {
    public static Consumer<AssertProvider<JsonPathValueAssert>> notNull() {
        return value -> assertThat(value).isNotNull();
    }

    public static Consumer<AssertProvider<JsonPathValueAssert>> equalsTo(
            MemberRegisterRequest memberRegisterRequest) {
        return value -> assertThat(value).isEqualTo(memberRegisterRequest.email());
    }
}
