package domaindriven.splearn.application;

import domaindriven.splearn.application.provided.MemberRegister;
import domaindriven.splearn.application.required.EmailSender;
import domaindriven.splearn.application.required.MemberRepository;
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
        Member member = Member.register(registerRequest, passwordEncoder);

        memberRepository.save(member);

        emailSender.send(member.getEmail(), "등록을 완료해주세요.", "아래 링크를 클릭해서 등록을 완료해주세요.");

        return member;
    }
}
