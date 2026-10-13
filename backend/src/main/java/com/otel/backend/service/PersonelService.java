package com.otel.backend.service;

import com.otel.backend.dto.PersonelDto;
import com.otel.backend.dto.PersonelIstegi;
import com.otel.backend.entity.Personel;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.PersonelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonelService {

    private final PersonelRepository repo;

    @Transactional(readOnly = true)
    public List<PersonelDto> listele() {
        return repo.findAll(Sort.by("id")).stream().map(PersonelDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PersonelDto getir(int id) {
        return PersonelDto.from(bul(id));
    }

    @Transactional
    public PersonelDto ekle(PersonelIstegi istek) {
        Personel p = new Personel();
        doldur(p, istek);
        return PersonelDto.from(repo.saveAndFlush(p));
    }

    @Transactional
    public PersonelDto guncelle(int id, PersonelIstegi istek) {
        Personel p = bul(id);
        doldur(p, istek);
        return PersonelDto.from(repo.saveAndFlush(p));
    }

    /**
     * Check-in, ödeme veya temizlik kaydı olan personel silinemez (FK) -> 409.
     * İşten ayrılan personel aktif=false yapılır (soft delete).
     */
    @Transactional
    public void sil(int id) {
        repo.delete(bul(id));
        repo.flush();
    }

    private Personel bul(int id) {
        return repo.findById(id).orElseThrow(() -> ApiHatasi.bulunamadi("Personel bulunamadı."));
    }

    private void doldur(Personel p, PersonelIstegi istek) {
        p.setAd(istek.ad().trim());
        p.setSoyad(istek.soyad().trim());
        p.setGorev(istek.gorev());
        p.setIseGirisTarihi(istek.iseGirisTarihi());
        p.setTelefon(istek.telefon());
        p.setAktif(istek.aktif() == null || istek.aktif());
    }
}