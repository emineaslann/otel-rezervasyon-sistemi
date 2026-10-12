package com.otel.backend.service;

import com.otel.backend.dto.OdaDurumIstegi;
import com.otel.backend.dto.TemizlikIstegi;
import com.otel.backend.entity.*;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.OdaRepository;
import com.otel.backend.repository.PersonelRepository;
import com.otel.backend.repository.TemizlikKaydiRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OdaService {

    private final OdaRepository odaRepo;
    private final PersonelRepository personelRepo;
    private final TemizlikKaydiRepository temizlikRepo;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> katGorevlileri() {
        return personelRepo.findByGorevAndAktifTrue(PersonelGorev.KAT_GOREVLISI).stream()
                .map(p -> Map.<String, Object>of(
                        "personelId", p.getId(),
                        "adSoyad", p.getAd() + " " + p.getSoyad()))
                .toList();
    }

    /**
     * Temizlik kaydı eklenir; odayı BOS yapan Java değil, veritabanındaki
     * trg_Temizlik_Tamamlandi_AI tetikleyicisidir.
     */
    @Transactional
    public Map<String, Object> temizlikTamamla(int odaId, TemizlikIstegi istek) {
        Oda oda = odaBul(odaId);
        if (oda.getDurum() != OdaDurum.TEMIZLIKTE) {
            throw ApiHatasi.cakisma("Yalnızca temizlikteki odalar için temizlik kaydı girilebilir. "
                    + "Oda şu an: " + oda.getDurum());
        }

        Personel personel = personelRepo.findById(istek.personelId())
                .filter(p -> p.getGorev() == PersonelGorev.KAT_GOREVLISI && p.getAktif())
                .orElseThrow(() -> ApiHatasi.gecersiz("Seçilen kişi aktif bir kat görevlisi değil."));

        TemizlikKaydi kayit = new TemizlikKaydi();
        kayit.setOda(oda);
        kayit.setPersonel(personel);
        kayit.setDurum(TemizlikDurum.TAMAMLANDI);
        kayit.setNotlar(istek.notlar());
        temizlikRepo.saveAndFlush(kayit);   // INSERT hemen çalışsın -> trigger devreye girsin

        // Trigger veritabanındaki odayı değiştirdi; Java'daki kopyayı tazele
        entityManager.refresh(oda);

        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("temizlikKaydiId", kayit.getId());
        cevap.put("odaNo", oda.getOdaNo());
        cevap.put("temizleyen", personel.getAd() + " " + personel.getSoyad());
        cevap.put("odaDurumu", oda.getDurum());
        return cevap;
    }

    /** Elle yalnızca bakıma alma ve bakımdan çıkarma yapılabilir. */
    @Transactional
    public Map<String, Object> durumDegistir(int odaId, OdaDurumIstegi istek) {
        Oda oda = odaBul(odaId);
        OdaDurum hedef = istek.durum();

        if (hedef == OdaDurum.BAKIMDA) {
            if (oda.getDurum() == OdaDurum.DOLU) {
                throw ApiHatasi.cakisma("Dolu bir oda bakıma alınamaz; önce check-out yapılmalı.");
            }
        } else if (hedef == OdaDurum.BOS) {
            if (oda.getDurum() != OdaDurum.BAKIMDA) {
                throw ApiHatasi.cakisma("Elle yalnızca bakımdaki oda BOS yapılabilir. "
                        + "Temizlikteki oda için temizlik kaydı girin.");
            }
        } else {
            throw ApiHatasi.gecersiz("DOLU ve TEMIZLIKTE durumlarını check-in ve check-out belirler; elle verilemez.");
        }

        oda.setDurum(hedef);
        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("odaId", oda.getId());
        cevap.put("odaNo", oda.getOdaNo());
        cevap.put("durum", oda.getDurum());
        return cevap;
    }

    private Oda odaBul(int odaId) {
        return odaRepo.findById(odaId)
                .orElseThrow(() -> ApiHatasi.bulunamadi("Oda bulunamadı."));
    }
}
