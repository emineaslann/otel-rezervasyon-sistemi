package com.otel.backend.controller;

import com.otel.backend.dto.CheckInIstegi;
import com.otel.backend.dto.CheckOutIstegi;
import com.otel.backend.security.OturumKullanicisi;
import com.otel.backend.service.ResepsiyonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.otel.backend.dto.OdaDurumIstegi;
import com.otel.backend.dto.TemizlikIstegi;
import com.otel.backend.service.OdaService;
import com.otel.backend.dto.MisafirIstegi;

/** /api/resepsiyon/** -> güvenlik ayarında RESEPSIYON ve YONETICI rollerine açık. */
@RestController
@RequestMapping("/api/resepsiyon")
@RequiredArgsConstructor
public class ResepsiyonController {

    private final ResepsiyonService service;
    private final OdaService odaService;

    /** VIEW (1): vw_BugunGirisCikis */
    @GetMapping("/bugun")
    public List<Map<String, Object>> bugun() {
        return service.bugun();
    }

    /** VIEW (2): vw_OdaGuncelDurum */
    @GetMapping("/oda-durumu")
    public List<Map<String, Object>> odaDurumu() {
        return service.odaDurumu();
    }

    @GetMapping("/aktif-konaklamalar")
    public List<Map<String, Object>> aktifKonaklamalar() {
        return service.aktifKonaklamalar();
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> checkIn(@AuthenticationPrincipal OturumKullanicisi ben,
                                       @Valid @RequestBody CheckInIstegi istek) {
        return service.checkIn(ben, istek);
    }

    /** TRANSACTION: sp_CheckOut */
    @PostMapping("/check-out")
    public Map<String, Object> checkOut(@AuthenticationPrincipal OturumKullanicisi ben,
                                        @Valid @RequestBody CheckOutIstegi istek) {
        return service.checkOut(ben, istek);
    }
    
    @GetMapping("/kat-gorevlileri")
    public List<Map<String, Object>> katGorevlileri() {
        return odaService.katGorevlileri();
    }

    /** Temizlik kaydı -> trigger odayı BOS yapar */
    @PostMapping("/odalar/{id}/temizlik")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> temizlik(@PathVariable int id, @Valid @RequestBody TemizlikIstegi istek) {
        return odaService.temizlikTamamla(id, istek);
    }

    /** Bakıma alma / bakımdan çıkarma */
    @PutMapping("/odalar/{id}/durum")
    public Map<String, Object> durum(@PathVariable int id, @Valid @RequestBody OdaDurumIstegi istek) {
        return odaService.durumDegistir(id, istek);
    }
    
    @GetMapping("/misafirler")
    public List<Map<String, Object>> misafirAra(@RequestParam(required = false) String q) {
        return service.misafirAra(q);
    }

    @PostMapping("/misafirler")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> misafirEkle(@Valid @RequestBody MisafirIstegi istek) {
        return service.misafirEkle(istek);
    }
}