package com.otel.backend.service;

import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.GorunumRepository;
import com.otel.backend.repository.ProsedurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RaporService {

    private final ProsedurRepository prosedurRepo;
    private final GorunumRepository gorunumRepo;

    /** PROSEDÜR (3): sp_AylikDolulukGelirRaporu. Yıl/ay verilmezse bu ay. */
    public Map<String, Object> aylik(Integer yil, Integer ay) {
        LocalDate bugun = LocalDate.now();
        int y = (yil == null) ? bugun.getYear() : yil;
        int a = (ay == null) ? bugun.getMonthValue() : ay;
        yilKontrol(y);
        if (a < 1 || a > 12) {
            throw ApiHatasi.gecersiz("Ay 1 ile 12 arasında olmalı.");
        }
        return prosedurRepo.aylikRapor(y, a);
    }

    public List<Map<String, Object>> yillik(Integer yil) {
        int y = (yil == null) ? LocalDate.now().getYear() : yil;
        yilKontrol(y);
        return prosedurRepo.yillikOzet(y);
    }

    public Map<String, Object> pano() {
        Map<String, Object> pano = new LinkedHashMap<>();
        pano.put("ozet", gorunumRepo.genelOzet());
        pano.put("odaDurumlari", gorunumRepo.odaDurumDagilimi());
        pano.put("populerHizmetler", gorunumRepo.populerHizmetler());
        return pano;
    }

    public List<Map<String, Object>> islemLog(int limit) {
        if (limit < 1 || limit > 1000) {
            throw ApiHatasi.gecersiz("limit 1 ile 1000 arasında olmalı.");
        }
        return gorunumRepo.islemLog(limit);
    }

    private void yilKontrol(int yil) {
        if (yil < 2000 || yil > 2100) {
            throw ApiHatasi.gecersiz("Yıl 2000 ile 2100 arasında olmalı.");
        }
    }
}