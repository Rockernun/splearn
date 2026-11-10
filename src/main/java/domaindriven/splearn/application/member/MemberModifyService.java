package domaindriven.splearn.application.member;

import domaindriven.splearn.application.member.provided.MemberFinder;
import domaindriven.splearn.application.member.provided.MemberRegister;
import domaindriven.splearn.application.member.required.EmailSender;
import domaindriven.splearn.application.member.required.MemberRepository;
import domaindriven.splearn.domain.member.DuplicateEmailException;
import domaindriven.splearn.domain.member.DuplicateProfileException;
import domaindriven.splearn.domain.member.MemberInfoUpdateRequest;
import domaindriven.splearn.domain.member.Profile;
import domaindriven.splearn.domain.shared.Email;
import domaindriven.splearn.domain.member.Member;
import domaindriven.splearn.application.member.provided.MemberRegisterRequest;
import domaindriven.splearn.domain.member.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class MemberModifyService implements MemberRegister {
    private final MemberFinder memberFinder;
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Member register(MemberRegisterRequest registerRequest) {
        checkDuplicateEmail(registerRequest);

        Member member = Member.register(registerRequest.toInfo(), passwordEncoder);

        memberRepository.save(member);

        sendWelcomeEmail(member);

        return member;
    }

    @Override
    public Member activate(Long memberId) {
        Member member = memberFinder.find(memberId);

        member.activate();

        return memberRepository.save(member);
    }

    @Override
    public Member deactivate(Long memberId) {
        Member member = memberFinder.find(memberId);

        member.deactivate();

        return memberRepository.save(member);
    }

    @Override
    public Member updateInfo(Long memberId, MemberInfoUpdateRequest memberInfoUpdateRequest) {
        Member member = memberFinder.find(memberId);

        checkDuplicateProfile(member, memberInfoUpdateRequest.profileAddress());

        member.updateInfo(memberInfoUpdateRequest);

        return memberRepository.save(member);
    }

    private void checkDuplicateProfile(Member member, String profileAddress) {
        if (profileAddress.isEmpty()) return;

        Profile currentProfile = member.getDetail().getProfile();
        if (currentProfile != null && currentProfile.address().equals(profileAddress)) return;

        if (memberRepository.findByProfile(new Profile(profileAddress)).isPresent()) {
            throw new DuplicateProfileException("이미 존재하는 프로필 주소입니다: " + profileAddress);
        }
    }

    private void sendWelcomeEmail(Member member) {
        emailSender.send(member.getEmail(), "등록을 완료해주세요.", "아래 링크를 클릭해서 등록을 완료해주세요.");
    }

    private void checkDuplicateEmail(MemberRegisterRequest registerRequest) {
        if (memberRepository.findByEmail(new Email(registerRequest.email())).isPresent()) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다: " + registerRequest.email());
        }
    }
}
