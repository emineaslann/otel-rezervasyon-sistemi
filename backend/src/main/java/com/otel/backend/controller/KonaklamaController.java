package com.otel.backend.controller;

import com.otel.backend.dto.HizmetEklemeIstegi;
import com.otel.backend.dto.OdemeIstegi;
import com.otel.backend.security.OturumKullanicisi;
import com.otel.backend.service.KonaklamaService;
import com.otel.backend.service.ResepsiyonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** /api/konaklamalar/** -> güvenlik ayarında RESEPSIYON ve YONETICI rollerine açık. */
@RestController
@RequestMapping("/api/konaklamalar")
@RequiredArgsConstructor
public class KonaklamaController {

    private final ResepsiyonService resepsiyonService;
    private final KonaklamaService konaklamaService;

    /** PROSEDÜR (2): sp_KonaklamaToplamTutar + ödemeler */
    @GetMapping("/{id}")
    public Map<String, Object> detay(@PathVariable int id) {
        return resepsiyonService.konaklamaDetay(id);
    }

    @PostMapping("/{id}/hizmetler")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> hizmetEkle(@PathVariable int id,
                                          @Valid @RequestBody HizmetEklemeIstegi istek) {
        return konaklamaService.hizmetEkle(id, istek);
    }

    @PostMapping("/{id}/odemeler")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> odemeAl(@AuthenticationPrincipal OturumKullanicisi ben,
                                       @PathVariable int id,
                                       @Valid @RequestBody OdemeIstegi istek) {
        return konaklamaService.odemeAl(ben, id, istek);
    }
}