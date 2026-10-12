package com.otel.backend.controller;

import com.otel.backend.dto.SezonFiyatiDto;
import com.otel.backend.dto.SezonFiyatiIstegi;
import com.otel.backend.service.SezonFiyatiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yonetim/sezon-fiyatlari")
@RequiredArgsConstructor
public class SezonFiyatiYonetimController {

    private final SezonFiyatiService service;

    /** Örnek: GET /api/yonetim/sezon-fiyatlari?odaTipiId=2 */
    @GetMapping
    public List<SezonFiyatiDto> listele(@RequestParam(required = false) Integer odaTipiId) {
        return service.listele(odaTipiId);
    }

    @GetMapping("/{id}")
    public SezonFiyatiDto getir(@PathVariable int id) { return service.getir(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SezonFiyatiDto ekle(@Valid @RequestBody SezonFiyatiIstegi istek) { return service.ekle(istek); }

    @PutMapping("/{id}")
    public SezonFiyatiDto guncelle(@PathVariable int id, @Valid @RequestBody SezonFiyatiIstegi istek) {
        return service.guncelle(id, istek);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@PathVariable int id) { service.sil(id); }
}