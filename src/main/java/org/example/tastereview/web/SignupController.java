package org.example.tastereview.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.tastereview.application.member.MemberService;
import org.example.tastereview.domain.exception.DuplicateEmailException;
import org.example.tastereview.domain.exception.DuplicateNicknameException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignupController {

    private final MemberService memberService;

    public SignupController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        try {
            memberService.signUp(form.getEmail(), form.getPassword(), form.getNickname());
        } catch (DuplicateEmailException e) {
            bindingResult.rejectValue("email", "duplicate.email", "이미 가입된 이메일입니다");
            return "auth/signup";
        } catch (DuplicateNicknameException e) {
            bindingResult.rejectValue("nickname", "duplicate.nickname", "이미 사용 중인 닉네임입니다");
            return "auth/signup";
        }
        return "redirect:/login?registered";
    }

    public static class MemberForm {

        @NotBlank(message = "이메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        private String email;

        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "비밀번호는 영문·숫자를 각각 1개 이상 포함한 8자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "닉네임을 입력해 주세요")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
        private String nickname;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}