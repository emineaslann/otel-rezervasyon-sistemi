package com.otel.backend.controller;

import com.otel.backend.dto.EkHizmetDto;
import com.otel.backend.dto.EkHizmetIstegi;
import com.otel.backend.service.EkHizmetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yonetim/ek-hizmetler")
@RequiredArgsConstructor
public class EkHizmetYonetimController {

    private final EkHizmetService service;

    @GetMapping
    public List<EkHizmetDto> listele() { return service.listele(); }

    @GetMapping("/{id}")
    public EkHizmetDto getir(@PathVariable int id) { return service.getir(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EkHizmetDto ekle(@Valid @RequestBody EkHizmetIstegi istek) { return service.ekle(istek); }

    @PutMapping("/{id}")
    public EkHizmetDto guncelle(@PathVariable int id, @Valid @RequestBody EkHizmetIstegi istek) {
        return service.guncelle(id, istek);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@PathVariable int id) { service.sil(id); }
}