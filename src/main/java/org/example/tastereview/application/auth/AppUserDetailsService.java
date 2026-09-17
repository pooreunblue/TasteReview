package org.example.tastereview.application.auth;

import lombok.RequiredArgsConstructor;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다"));
        return new MemberPrincipal(
                member.getId(), member.getEmail(), member.getPasswordHash(),
                member.getNickname(), member.getRole());
    }
}