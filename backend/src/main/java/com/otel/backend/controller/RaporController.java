package com.otel.backend.controller;

import com.otel.backend.service.RaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** /api/raporlar/** -> güvenlik ayarında yalnızca YONETICI rolüne açık. */
@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
public class RaporController {

    private final RaporService service;

    /** PROSEDÜR (3). Örnek: GET /api/raporlar/aylik?yil=2026&ay=8 */
    @GetMapping("/aylik")
    public Map<String, Object> aylik(@RequestParam(required = false) Integer yil,
                                     @RequestParam(required = false) Integer ay) {
        return service.aylik(yil, ay);
    }

    @GetMapping("/yillik")
    public List<Map<String, Object>> yillik(@RequestParam(required = false) Integer yil) {
        return service.yillik(yil);
    }

    @GetMapping("/pano")
    public Map<String, Object> pano() {
        return service.pano();
    }
}