package domaindriven.splearn.adapter.webapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domaindriven.splearn.application.member.provided.MemberRegister;
import domaindriven.splearn.domain.member.Member;
import domaindriven.splearn.domain.member.MemberFixture;
import domaindriven.splearn.application.member.provided.MemberRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberApi.class)
@RequiredArgsConstructor
class MemberApiWebMvcTest {
    final MockMvcTester mvcTester;
    final ObjectMapper objectMapper;

    @MockitoBean
    private MemberRegister memberRegister;

    @Test
    void register() {
        Member member = MemberFixture.createMember(1L);
        MemberRegisterRequest memberRegisterRequest = MemberFixture.createMemberRegisterRequest();

        when(memberRegister.register(any())).thenReturn(member);
        String requestJson = objectMapper.writeValueAsString(memberRegisterRequest);

        assertThat(mvcTester.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.memberId").asNumber().isEqualTo(1);

        verify(memberRegister).register(memberRegisterRequest);
    }

    @Test
    void registerFail() {
        MemberRegisterRequest memberRegisterRequest = MemberFixture.createMemberRegisterRequest("invalid email");
        String requestJson = objectMapper.writeValueAsString(memberRegisterRequest);

        assertThat(mvcTester.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}