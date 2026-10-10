package com.otel.backend.controller;

import com.otel.backend.dto.RezervasyonIstegi;
import com.otel.backend.security.OturumKullanicisi;
import com.otel.backend.service.RezervasyonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rezervasyonlar")
@RequiredArgsConstructor
public class RezervasyonController {

    private final RezervasyonService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MISAFIR', 'RESEPSIYON', 'YONETICI')")
    public Map<String, Object> olustur(@AuthenticationPrincipal OturumKullanicisi ben,
                                       @Valid @RequestBody RezervasyonIstegi istek) {
        return service.olustur(ben, istek);
    }

    @GetMapping("/benim")
    @PreAuthorize("hasRole('MISAFIR')")
    public List<Map<String, Object>> benim(@AuthenticationPrincipal OturumKullanicisi ben) {
        return service.benim(ben);
    }

    @GetMapping("/gecmis")
    @PreAuthorize("hasRole('MISAFIR')")
    public List<Map<String, Object>> gecmis(@AuthenticationPrincipal OturumKullanicisi ben) {
        return service.gecmis(ben);
    }

    @PutMapping("/{id}/iptal")
    @PreAuthorize("hasAnyRole('MISAFIR', 'RESEPSIYON', 'YONETICI')")
    public Map<String, Object> iptal(@AuthenticationPrincipal OturumKullanicisi ben,
                                     @PathVariable int id) {
        return service.iptal(ben, id);
    }
}