package org.example.tastereview.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) boolean error,
                        @RequestParam(required = false) boolean registered,
                        Model model) {
        if (error) {
            model.addAttribute("loginError", "이메일 또는 비밀번호가 올바르지 않습니다");
        }
        if (registered) {
            model.addAttribute("registeredMessage", "가입이 완료되었습니다. 로그인해 주세요");
        }
        return "auth/login";
    }
}