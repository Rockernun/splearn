package domaindriven.splearn.application;

import domaindriven.splearn.application.provided.MemberRegister;
import domaindriven.splearn.application.required.EmailSender;
import domaindriven.splearn.application.required.MemberRepository;
import domaindriven.splearn.domain.DuplicateEmailException;
import domaindriven.splearn.domain.Email;
import domaindriven.splearn.domain.Member;
import domaindriven.splearn.domain.MemberRegisterRequest;
import domaindriven.splearn.domain.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService implements MemberRegister {
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Member register(MemberRegisterRequest registerRequest) {
        checkDuplicateEmail(registerRequest);

        Member member = Member.register(registerRequest, passwordEncoder);

        memberRepository.save(member);

        sendWelcomeEmail(member);

        return member;
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
