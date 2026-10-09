package com.otel.backend.controller;

import com.otel.backend.dto.*;
import com.otel.backend.security.OturumKullanicisi;
import com.otel.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/giris")
    public GirisCevabi giris(@Valid @RequestBody GirisIstegi istek) {
        return authService.giris(istek);
    }

    @PostMapping("/kayit")
    @ResponseStatus(HttpStatus.CREATED)
    public GirisCevabi kayit(@Valid @RequestBody KayitIstegi istek) {
        return authService.kayit(istek);
    }

    @GetMapping("/ben")
    public KullaniciBilgisi ben(@AuthenticationPrincipal OturumKullanicisi oturum) {
        return authService.ben(oturum);
    }
}