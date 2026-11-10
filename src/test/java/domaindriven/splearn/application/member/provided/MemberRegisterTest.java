package domaindriven.splearn.application.member.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import domaindriven.splearn.SplearnTestConfiguration;
import domaindriven.splearn.domain.member.DuplicateEmailException;
import domaindriven.splearn.domain.member.DuplicateProfileException;
import domaindriven.splearn.domain.member.Member;
import domaindriven.splearn.domain.member.MemberFixture;
import domaindriven.splearn.domain.member.MemberInfoUpdateRequest;
import domaindriven.splearn.domain.member.MemberRegisterRequest;
import domaindriven.splearn.domain.member.MemberStatus;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(SplearnTestConfiguration.class)
record MemberRegisterTest(MemberRegister memberRegister, EntityManager entityManager) {

    @Test
    void register() {
        Member member = memberRegister.register(MemberFixture.createMemberRegisterRequest());

        assertThat(member.getId()).isNotNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    void duplicateEmailFail() {
        memberRegister.register(MemberFixture.createMemberRegisterRequest());

        assertThatThrownBy(() -> memberRegister.register(MemberFixture.createMemberRegisterRequest()))
                .isInstanceOf(DuplicateEmailException.class);
    }
    
    @Test
    void activate() {
        Member member = registerMember();

        member = memberRegister.activate(member.getId());
        entityManager.flush();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getDetail().getActivatedAt()).isNotNull();
    }

    @Test
    void deactivate() {
        Member member = registerMember();

        member = memberRegister.activate(member.getId());
        entityManager.flush();
        entityManager.clear();

        member = memberRegister.deactivate(member.getId());

        assertThat(member.getStatus()).isEqualTo(MemberStatus.DEACTIVATED);
        assertThat(member.getDetail().getDeactivatedAt()).isNotNull();
    }

    private Member registerMember() {
        Member member = memberRegister.register(MemberFixture.createMemberRegisterRequest());
        entityManager.flush();
        entityManager.clear();
        return member;
    }

    private Member registerMember(String email) {
        Member member = memberRegister.register(MemberFixture.createMemberRegisterRequest(email));
        entityManager.flush();
        entityManager.clear();
        return member;
    }

    @Test
    void updateInfo() {
        Member member = registerMember();

        memberRegister.activate(member.getId());
        entityManager.flush();
        entityManager.clear();

        member = memberRegister.updateInfo(member.getId(), new MemberInfoUpdateRequest("Eveveryhandsome", "rockeraddress", "자기소개"));

        assertThat(member.getDetail().getProfile().address()).isEqualTo("rockeraddress");
    }

    @Test
    void updateInfoFail() {
        Member member1 = registerMember();
        memberRegister.activate(member1.getId());
        memberRegister.updateInfo(member1.getId(), new MemberInfoUpdateRequest("Eveveryhandsome", "rockeraddress", "자기소개"));

        Member member2 = registerMember("byunguk2@splearn.com");
        memberRegister.activate(member2.getId());
        entityManager.flush();
        entityManager.clear();

        // member2는 기존의 member1과 같은 프로필을 사용할 수 없다
        assertThatThrownBy(() -> {
            memberRegister.updateInfo(member2.getId(), new MemberInfoUpdateRequest("aareveryhandsome", "rockeraddress", "introduction"));
        }).isInstanceOf(DuplicateProfileException.class);

        // 다른 프로필 주소로는 변경 가능
        memberRegister.updateInfo(member2.getId(), new MemberInfoUpdateRequest("aareveryhandsome", "rockeraddress1", "introduction"));

        // 본인 현재 주소와 같은 값으로 수정하는 경우는 중복 검사에서 제외된다
        memberRegister.updateInfo(member1.getId(), new MemberInfoUpdateRequest("aareveryhandsome", "rockeraddress", "introduction"));

        // 프로필 주소를 제거하는 것도 가능
        memberRegister.updateInfo(member1.getId(), new MemberInfoUpdateRequest("aareveryhandsome", "", "introduction"));
        memberRegister.updateInfo(member2.getId(), new MemberInfoUpdateRequest("aareveryhandsome", "", "introduction"));
        entityManager.flush();
    }

    @Test
    void memberRegisterRequestFail() {
        checkValidation(new MemberRegisterRequest("byunguk@splearn.com", "rock", "veryverysecret"));
        checkValidation(new MemberRegisterRequest("byunguk@splearn.com", "rockernun", "secret"));
        checkValidation(new MemberRegisterRequest("byunguksplearn.com", "rockernun", "veryverysecret"));
    }

    private void checkValidation(MemberRegisterRequest invalid) {
        assertThatThrownBy(() -> memberRegister.register(invalid))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
