package domaindriven.splearn.application.member.required;

import static domaindriven.splearn.domain.member.MemberFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import domaindriven.splearn.domain.member.Member;
import domaindriven.splearn.domain.member.MemberStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void createMember() {
        Member member = Member.register(createMemberRegisterRequest().toInfo(), createPasswordEncoder());

        assertThat(member.getId()).isNull();

        memberRepository.save(member);

        assertThat(member.getId()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        var found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(found.getDetail().getRegisteredAt()).isNotNull();
    }
    
    @Test
    void duplicateEmailFail() {
        Member member1 = Member.register(createMemberRegisterRequest().toInfo(), createPasswordEncoder());
        memberRepository.save(member1);

        Member member2 = Member.register(createMemberRegisterRequest().toInfo(), createPasswordEncoder());

        assertThatThrownBy(() -> memberRepository.save(member2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}