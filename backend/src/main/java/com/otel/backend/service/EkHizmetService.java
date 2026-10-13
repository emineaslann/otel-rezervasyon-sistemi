package com.otel.backend.service;

import com.otel.backend.dto.EkHizmetDto;
import com.otel.backend.dto.EkHizmetIstegi;
import com.otel.backend.entity.EkHizmet;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.EkHizmetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EkHizmetService {

    private final EkHizmetRepository repo;

    /** Yönetici pasif hizmetleri de görür (herkese açık liste sadece aktifleri gösterir). */
    @Transactional(readOnly = true)
    public List<EkHizmetDto> listele() {
        return repo.findAll(Sort.by("hizmetAdi")).stream().map(EkHizmetDto::from).toList();
    }

    @Transactional(readOnly = true)
    public EkHizmetDto getir(int id) {
        return EkHizmetDto.from(bul(id));
    }

    @Transactional
    public EkHizmetDto ekle(EkHizmetIstegi istek) {
        EkHizmet h = new EkHizmet();
        doldur(h, istek);
        return EkHizmetDto.from(repo.saveAndFlush(h));
    }

    @Transactional
    public EkHizmetDto guncelle(int id, EkHizmetIstegi istek) {
        EkHizmet h = bul(id);
        doldur(h, istek);
        return EkHizmetDto.from(repo.saveAndFlush(h));
    }

    /** Konaklamalarda satılmış hizmet silinemez (FK) -> 409; pasif yapılmalı. */
    @Transactional
    public void sil(int id) {
        repo.delete(bul(id));
        repo.flush();
    }

    private EkHizmet bul(int id) {
        return repo.findById(id).orElseThrow(() -> ApiHatasi.bulunamadi("Ek hizmet bulunamadı."));
    }

    private void doldur(EkHizmet h, EkHizmetIstegi istek) {
        h.setHizmetAdi(istek.hizmetAdi().trim());
        h.setBirimFiyat(istek.birimFiyat());
        h.setAktif(istek.aktif() == null || istek.aktif());   // gönderilmezse aktif
    }
}