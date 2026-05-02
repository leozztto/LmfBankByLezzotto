package com.lezztto.LmfBank.auth.controller;

import com.lezztto.LmfBank.auth.domain.LoginRequest;
import com.lezztto.LmfBank.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        return jwtService.generateToken(request.username());
    }
}
