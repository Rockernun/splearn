package domaindriven.splearn.adapter.webapi;

import static domaindriven.splearn.AssertThatUtils.equalsTo;
import static domaindriven.splearn.AssertThatUtils.notNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import domaindriven.splearn.adapter.webapi.dto.MemberRegisterResponse;
import domaindriven.splearn.application.member.provided.MemberRegister;
import domaindriven.splearn.application.member.required.MemberRepository;
import domaindriven.splearn.domain.member.DuplicateEmailException;
import domaindriven.splearn.domain.member.Member;
import domaindriven.splearn.domain.member.MemberFixture;
import domaindriven.splearn.domain.member.MemberRegisterRequest;
import domaindriven.splearn.domain.member.MemberStatus;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonPathValueAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor
public class MemberApiTest {
    final MockMvcTester mvcTester;
    final ObjectMapper objectMapper;
    final MemberRepository memberRepository;
    final MemberRegister memberRegister;

    @Test
    void register() throws UnsupportedEncodingException {
        MemberRegisterRequest memberRegisterRequest = MemberFixture.createMemberRegisterRequest();
        String requestJson = objectMapper.writeValueAsString(memberRegisterRequest);

        MvcTestResult result = mvcTester.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(requestJson).exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.memberId", notNull())
                .hasPathSatisfying("$.email", equalsTo(memberRegisterRequest));

        MemberRegisterResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), MemberRegisterResponse.class);

        Member member = memberRepository.findById(response.memberId()).orElseThrow();

        assertThat(member.getEmail().address()).isEqualTo(memberRegisterRequest.email());
        assertThat(member.getNickname()).isEqualTo(memberRegisterRequest.nickname());
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }
    
    @Test
    void duplicateEmail() {
        memberRegister.register(MemberFixture.createMemberRegisterRequest());

        MemberRegisterRequest memberRegisterRequest = MemberFixture.createMemberRegisterRequest();
        String requestJson = objectMapper.writeValueAsString(memberRegisterRequest);

        MvcTestResult result = mvcTester.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(requestJson).exchange();

        assertThat(result)
                .apply(print())
                .hasStatus(HttpStatus.CONFLICT);
    }
}
