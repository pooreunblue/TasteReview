package org.example.tastereview.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.example.tastereview.application.member.MemberService;
import org.example.tastereview.config.SecurityConfig;
import org.example.tastereview.domain.exception.DuplicateEmailException;
import org.example.tastereview.domain.exception.DuplicateNicknameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignupController.class)
@Import(SecurityConfig.class)
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 화면을 GET하면 200과 함께 가입 폼을 렌더링한다")
    void givenSignupPage_whenGet_thenRendersSignupForm() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeExists("memberForm"));
    }

    @Test
    @DisplayName("유효한 가입 요청이면 로그인 화면으로 리다이렉트한다")
    void givenValidSignup_whenPost_thenRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "a@b.com")
                        .param("password", "pass1234")
                        .param("nickname", "맛집러버"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    @DisplayName("검증 위반이면 200과 함께 필드 에러를 렌더링한다")
    void givenInvalidSignup_whenPost_thenRendersFieldErrors() throws Exception {
        mockMvc.perform(post("/signup").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("memberForm", "email"))
                .andExpect(model().attributeHasFieldErrors("memberForm", "password"))
                .andExpect(model().attributeHasFieldErrors("memberForm", "nickname"));
    }

    @Test
    @DisplayName("이메일이 중복이면 사유를 표시하고 입력값을 유지한 채 재렌더링한다")
    void givenDuplicateEmail_whenPost_thenShowsEmailErrorAndRerenders() throws Exception {
        willThrow(DuplicateEmailException.class)
                .given(memberService)
                .signUp(any(), any(), any());

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "a@b.com")
                        .param("password", "pass1234")
                        .param("nickname", "맛집러버"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("memberForm", "email"))
                .andExpect(content().string(containsString("이미 가입된 이메일입니다")));
    }

    @Test
    @DisplayName("닉네임이 중복이면 사유를 표시하고 입력값을 유지한 채 재렌더링한다")
    void givenDuplicateNickname_whenPost_thenShowsNicknameErrorAndRerenders() throws Exception {
        willThrow(DuplicateNicknameException.class)
                .given(memberService)
                .signUp(any(), any(), any());

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "a@b.com")
                        .param("password", "pass1234")
                        .param("nickname", "맛집러버"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("memberForm", "nickname"))
                .andExpect(content().string(containsString("이미 사용 중인 닉네임입니다")));
    }
}