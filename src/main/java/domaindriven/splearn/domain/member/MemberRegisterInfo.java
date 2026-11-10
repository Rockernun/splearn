package domaindriven.splearn.domain.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record MemberRegisterInfo(
        @Email String email,
        @Size(min = 5, max = 20) String nickname,
        @Size(min = 8, max = 100) String password
) {
}
