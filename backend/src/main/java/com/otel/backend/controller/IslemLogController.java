package com.otel.backend.controller;

import com.otel.backend.service.RaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Tetikleyicilerin doldurduğu işlem günlüğü (yalnızca YONETICI). */
@RestController
@RequestMapping("/api/yonetim/islem-log")
@RequiredArgsConstructor
public class IslemLogController {

    private final RaporService service;

    @GetMapping
    public List<Map<String, Object>> listele(@RequestParam(defaultValue = "100") int limit) {
        return service.islemLog(limit);
    }
}