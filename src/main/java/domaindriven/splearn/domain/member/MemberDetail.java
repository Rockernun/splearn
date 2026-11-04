package domaindriven.splearn.domain.member;

import static org.springframework.util.Assert.state;

import domaindriven.splearn.domain.AbstractEntity;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberDetail extends AbstractEntity {
    private String profile;

    private String introduction;

    private LocalDateTime registeredAt;

    private LocalDateTime activatedAt;

    private LocalDateTime deactivatedAt;

    static MemberDetail create() {
        MemberDetail memberDetail = new MemberDetail();
        memberDetail.registeredAt = LocalDateTime.now();
        return memberDetail;
    }

    void activated() {
        Assert.isTrue(activatedAt == null, "이미 activatedAt은 결정되었습니다.");
        this.activatedAt = LocalDateTime.now();
    }

    void deactivated() {
        Assert.isTrue(deactivatedAt == null, "이미 deactivatedAt은 결정되었습니다.");
        this.deactivatedAt = LocalDateTime.now();
    }
}
