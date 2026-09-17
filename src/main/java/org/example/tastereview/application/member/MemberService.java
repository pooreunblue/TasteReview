package org.example.tastereview.application.member;

import lombok.RequiredArgsConstructor;
import org.example.tastereview.domain.exception.DuplicateEmailException;
import org.example.tastereview.domain.exception.DuplicateNicknameException;
import org.example.tastereview.domain.exception.MemberNotFoundException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member signUp(String email, String rawPassword, String nickname) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 영문·숫자를 각각 1개 이상 포함한 8자 이상이어야 합니다");
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다");
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다");
        }
        Member member = new Member(normalizedEmail, passwordEncoder.encode(rawPassword), nickname);
        return memberRepository.save(member);
    }

    public Member findById(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}