package com.otel.backend.controller;

import com.otel.backend.dto.OdaDto;
import com.otel.backend.dto.OdaIstegi;
import com.otel.backend.service.OdaYonetimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yonetim/odalar")
@RequiredArgsConstructor
public class OdaYonetimController {

    private final OdaYonetimService service;

    @GetMapping
    public List<OdaDto> listele() { return service.listele(); }

    @GetMapping("/{id}")
    public OdaDto getir(@PathVariable int id) { return service.getir(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OdaDto ekle(@Valid @RequestBody OdaIstegi istek) { return service.ekle(istek); }

    @PutMapping("/{id}")
    public OdaDto guncelle(@PathVariable int id, @Valid @RequestBody OdaIstegi istek) {
        return service.guncelle(id, istek);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@PathVariable int id) { service.sil(id); }
}