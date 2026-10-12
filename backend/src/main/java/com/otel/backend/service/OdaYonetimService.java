package com.otel.backend.service;

import com.otel.backend.dto.OdaDto;
import com.otel.backend.dto.OdaIstegi;
import com.otel.backend.entity.Oda;
import com.otel.backend.entity.OdaTipi;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.OdaRepository;
import com.otel.backend.repository.OdaTipiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OdaYonetimService {

    private final OdaRepository repo;
    private final OdaTipiRepository odaTipiRepo;

    @Transactional(readOnly = true)
    public List<OdaDto> listele() {
        return repo.findAll(Sort.by("odaNo")).stream().map(OdaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OdaDto getir(int id) {
        return OdaDto.from(bul(id));
    }

    /** Yeni oda BOS başlar (veritabanındaki DEFAULT). Durumu trigger'lar yönetir. */
    @Transactional
    public OdaDto ekle(OdaIstegi istek) {
        Oda o = new Oda();
        doldur(o, istek);
        return OdaDto.from(repo.saveAndFlush(o));
    }

    @Transactional
    public OdaDto guncelle(int id, OdaIstegi istek) {
        Oda o = bul(id);
        doldur(o, istek);
        return OdaDto.from(repo.saveAndFlush(o));
    }

    /** Rezervasyonu olan oda silinemez (FK) -> 409 */
    @Transactional
    public void sil(int id) {
        repo.delete(bul(id));
        repo.flush();
    }

    private Oda bul(int id) {
        return repo.findById(id).orElseThrow(() -> ApiHatasi.bulunamadi("Oda bulunamadı."));
    }

    private void doldur(Oda o, OdaIstegi istek) {
        OdaTipi tip = odaTipiRepo.findById(istek.odaTipiId())
                .orElseThrow(() -> ApiHatasi.gecersiz("Seçilen oda tipi bulunamadı."));
        o.setOdaNo(istek.odaNo().trim());
        o.setKat(istek.kat());
        o.setOdaTipi(tip);
    }
}