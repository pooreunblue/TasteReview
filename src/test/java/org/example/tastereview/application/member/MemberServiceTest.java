package org.example.tastereview.application.member;

import org.example.tastereview.domain.exception.DuplicateEmailException;
import org.example.tastereview.domain.exception.DuplicateNicknameException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MemberService memberService() {
        return new MemberService(memberRepository, passwordEncoder);
    }

    @Test
    @DisplayName("중복이 없으면 이메일을 소문자로 정규화하고 해시로 비밀번호를 저장한다")
    void givenNoDuplicate_whenSignUp_thenMemberSavedWithNormalizedEmailAndHash() {
        given(memberRepository.existsByEmail("user@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("tester")).willReturn(false);
        given(memberRepository.save(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Member saved = memberService().signUp("  User@Example.COM  ", "pass1234", "tester");

        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getNickname()).isEqualTo("tester");
        assertThat(saved.getPasswordHash()).isNotEqualTo("pass1234");
        assertThat(passwordEncoder.matches("pass1234", saved.getPasswordHash())).isTrue();
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("이메일이 이미 있으면 가입을 거부하고 저장하지 않는다")
    void givenDuplicatedEmail_whenSignUp_thenDuplicateEmailException() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> memberService().signUp(" a@b.com ", "abc12345", "tester"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 가입된 이메일입니다");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("닉네임이 이미 있으면 가입을 거부하고 저장하지 않는다")
    void givenDuplicatedNickname_whenSignUp_thenDuplicateNicknameException() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(false);
        given(memberRepository.existsByNickname("tester")).willReturn(true);

        assertThatThrownBy(() -> memberService().signUp("a@b.com", "abc12345", "tester"))
                .isInstanceOf(DuplicateNicknameException.class)
                .hasMessage("이미 사용 중인 닉네임입니다");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "12345678", "abc1234"})
    @DisplayName("비밀번호가 영문·숫자를 포함한 8자 이상이 아니면 가입을 거부한다")
    void givenWeakPassword_whenSignUp_thenIllegalArgumentException(String rawPassword) {
        assertThatThrownBy(() -> memberService().signUp("a@b.com", rawPassword, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문·숫자를 각각 1개 이상 포함한 8자 이상이어야 합니다");
    }
}