package com.otel.backend.service;

import com.otel.backend.dto.SezonFiyatiDto;
import com.otel.backend.dto.SezonFiyatiIstegi;
import com.otel.backend.entity.OdaTipi;
import com.otel.backend.entity.SezonFiyati;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.OdaTipiRepository;
import com.otel.backend.repository.SezonFiyatiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SezonFiyatiService {

    private final SezonFiyatiRepository repo;
    private final OdaTipiRepository odaTipiRepo;

    /** odaTipiId verilirse sadece o tipin sezonları */
    @Transactional(readOnly = true)
    public List<SezonFiyatiDto> listele(Integer odaTipiId) {
        List<SezonFiyati> liste = (odaTipiId == null)
                ? repo.findAll(Sort.by("baslangicTarihi"))
                : repo.findByOdaTipi_IdOrderByBaslangicTarihiAsc(odaTipiId);
        return liste.stream().map(SezonFiyatiDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SezonFiyatiDto getir(int id) {
        return SezonFiyatiDto.from(bul(id));
    }

    /** Aynı oda tipi için çakışan sezon -> trg_SezonFiyati_Cakisma_BI -> 409 */
    @Transactional
    public SezonFiyatiDto ekle(SezonFiyatiIstegi istek) {
        SezonFiyati s = new SezonFiyati();
        doldur(s, istek);
        return SezonFiyatiDto.from(repo.saveAndFlush(s));
    }

    @Transactional
    public SezonFiyatiDto guncelle(int id, SezonFiyatiIstegi istek) {
        SezonFiyati s = bul(id);
        doldur(s, istek);
        return SezonFiyatiDto.from(repo.saveAndFlush(s));
    }

    @Transactional
    public void sil(int id) {
        repo.delete(bul(id));
        repo.flush();
    }

    private SezonFiyati bul(int id) {
        return repo.findById(id).orElseThrow(() -> ApiHatasi.bulunamadi("Sezon fiyatı bulunamadı."));
    }

    private void doldur(SezonFiyati s, SezonFiyatiIstegi istek) {
        if (istek.bitisTarihi().isBefore(istek.baslangicTarihi())) {
            throw ApiHatasi.gecersiz("Bitiş tarihi başlangıç tarihinden önce olamaz.");
        }
        OdaTipi tip = odaTipiRepo.findById(istek.odaTipiId())
                .orElseThrow(() -> ApiHatasi.gecersiz("Seçilen oda tipi bulunamadı."));
        s.setOdaTipi(tip);
        s.setSezonAdi(istek.sezonAdi().trim());
        s.setBaslangicTarihi(istek.baslangicTarihi());
        s.setBitisTarihi(istek.bitisTarihi());
        s.setGecelikFiyat(istek.gecelikFiyat());
    }
}