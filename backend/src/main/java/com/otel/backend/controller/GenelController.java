package com.otel.backend.controller;

import com.otel.backend.dto.EkHizmetDto;
import com.otel.backend.dto.OdaTipiDto;
import com.otel.backend.service.GenelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Herkese açık uç noktalar (güvenlik ayarında permitAll). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GenelController {

    private final GenelService genelService;

    @GetMapping("/oda-tipleri")
    public List<OdaTipiDto> odaTipleri() {
        return genelService.odaTipleri();
    }

    @GetMapping("/ek-hizmetler")
    public List<EkHizmetDto> ekHizmetler() {
        return genelService.ekHizmetler();
    }

    /** Örnek: GET /api/odalar/bos?giris=2026-11-10&cikis=2026-11-13&odaTipiId=2&kisi=2 */
    @GetMapping("/odalar/bos")
    public List<Map<String, Object>> bosOdalar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate giris,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cikis,
            @RequestParam(required = false) Integer odaTipiId,
            @RequestParam(required = false) Integer kisi) {
        return genelService.bosOdalar(giris, cikis, odaTipiId, kisi);
    }
}