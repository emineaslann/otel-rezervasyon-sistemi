package com.otel.backend.service;

import com.otel.backend.dto.OdaTipiDto;
import com.otel.backend.dto.OdaTipiIstegi;
import com.otel.backend.entity.OdaTipi;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.OdaTipiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OdaTipiService {

    private final OdaTipiRepository repo;

    @Transactional(readOnly = true)
    public List<OdaTipiDto> listele() {
        return repo.findAll(Sort.by("id")).stream().map(OdaTipiDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OdaTipiDto getir(int id) {
        return OdaTipiDto.from(bul(id));
    }

    @Transactional
    public OdaTipiDto ekle(OdaTipiIstegi istek) {
        OdaTipi t = new OdaTipi();
        doldur(t, istek);
        return OdaTipiDto.from(repo.saveAndFlush(t));
    }

    @Transactional
    public OdaTipiDto guncelle(int id, OdaTipiIstegi istek) {
        OdaTipi t = bul(id);
        doldur(t, istek);
        return OdaTipiDto.from(repo.saveAndFlush(t));
    }

    /** Bu tipte oda varsa veritabanındaki FK (ON DELETE RESTRICT) silmeyi engeller -> 409 */
    @Transactional
    public void sil(int id) {
        repo.delete(bul(id));
        repo.flush();
    }

    // ------------------------------------------------------------ yardımcılar

    private OdaTipi bul(int id) {
        return repo.findById(id).orElseThrow(() -> ApiHatasi.bulunamadi("Oda tipi bulunamadı."));
    }

    private void doldur(OdaTipi t, OdaTipiIstegi istek) {
        t.setTipAdi(istek.tipAdi().trim());
        t.setKapasite(istek.kapasite());
        t.setAciklama(istek.aciklama());
        t.setTemelFiyat(istek.temelFiyat());
    }
}