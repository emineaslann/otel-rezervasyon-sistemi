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

/** /api/resepsiyon/** -> güvenlik ayarında RESEPSIYON ve YONETICI rollerine açık. */
@RestController
@RequestMapping("/api/resepsiyon")
@RequiredArgsConstructor
public class ResepsiyonController {

    private final ResepsiyonService service;

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
}