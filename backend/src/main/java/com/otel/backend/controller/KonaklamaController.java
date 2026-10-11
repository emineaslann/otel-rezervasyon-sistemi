package com.otel.backend.controller;

import com.otel.backend.service.ResepsiyonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** /api/konaklamalar/** -> güvenlik ayarında RESEPSIYON ve YONETICI rollerine açık. */
@RestController
@RequestMapping("/api/konaklamalar")
@RequiredArgsConstructor
public class KonaklamaController {

    private final ResepsiyonService resepsiyonService;

    /** PROSEDÜR (2): sp_KonaklamaToplamTutar + ödemeler */
    @GetMapping("/{id}")
    public Map<String, Object> detay(@PathVariable int id) {
        return resepsiyonService.konaklamaDetay(id);
    }
}