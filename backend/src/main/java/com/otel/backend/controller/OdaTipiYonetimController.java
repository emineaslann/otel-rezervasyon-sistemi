package com.otel.backend.controller;

import com.otel.backend.dto.OdaTipiDto;
import com.otel.backend.dto.OdaTipiIstegi;
import com.otel.backend.service.OdaTipiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/yonetim/** -> güvenlik ayarında yalnızca YONETICI rolüne açık. */
@RestController
@RequestMapping("/api/yonetim/oda-tipleri")
@RequiredArgsConstructor
public class OdaTipiYonetimController {

    private final OdaTipiService service;

    @GetMapping
    public List<OdaTipiDto> listele() {
        return service.listele();
    }

    @GetMapping("/{id}")
    public OdaTipiDto getir(@PathVariable int id) {
        return service.getir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OdaTipiDto ekle(@Valid @RequestBody OdaTipiIstegi istek) {
        return service.ekle(istek);
    }

    @PutMapping("/{id}")
    public OdaTipiDto guncelle(@PathVariable int id, @Valid @RequestBody OdaTipiIstegi istek) {
        return service.guncelle(id, istek);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@PathVariable int id) {
        service.sil(id);
    }
}