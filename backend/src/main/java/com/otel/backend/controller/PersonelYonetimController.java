package com.otel.backend.controller;

import com.otel.backend.dto.PersonelDto;
import com.otel.backend.dto.PersonelIstegi;
import com.otel.backend.service.PersonelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yonetim/personel")
@RequiredArgsConstructor
public class PersonelYonetimController {

    private final PersonelService service;

    @GetMapping
    public List<PersonelDto> listele() { return service.listele(); }

    @GetMapping("/{id}")
    public PersonelDto getir(@PathVariable int id) { return service.getir(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonelDto ekle(@Valid @RequestBody PersonelIstegi istek) { return service.ekle(istek); }

    @PutMapping("/{id}")
    public PersonelDto guncelle(@PathVariable int id, @Valid @RequestBody PersonelIstegi istek) {
        return service.guncelle(id, istek);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@PathVariable int id) { service.sil(id); }
}