package com.otel.backend.service;

import com.otel.backend.dto.*;
import com.otel.backend.entity.Kullanici;
import com.otel.backend.entity.Misafir;
import com.otel.backend.entity.Rol;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.KullaniciRepository;
import com.otel.backend.repository.MisafirRepository;
import com.otel.backend.security.JwtServisi;
import com.otel.backend.security.OturumKullanicisi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KullaniciRepository kullaniciRepo;
    private final MisafirRepository misafirRepo;
    private final PasswordEncoder sifreKodlayici;
    private final JwtServisi jwtServisi;

    @Transactional
    public GirisCevabi giris(GirisIstegi istek) {
        Kullanici k = kullaniciRepo.findByKullaniciAdi(istek.kullaniciAdi().trim())
                .orElse(null);

        // Kullanıcı yok da olsa şifre yanlış da olsa AYNI mesaj:
        // saldırgan hangi kullanıcı adlarının var olduğunu öğrenemesin.
        if (k == null || !sifreKodlayici.matches(istek.sifre(), k.getSifreHash())) {
            throw ApiHatasi.yetkisiz("Kullanıcı adı veya şifre hatalı.");
        }
        if (!k.getAktif()) {
            throw ApiHatasi.yasak("Hesabınız pasif durumda. Lütfen otel yönetimiyle iletişime geçin.");
        }

        k.setSonGiris(LocalDateTime.now());   // @Transactional sayesinde otomatik kaydedilir
        return cevapOlustur(k);
    }

    /**
     * Misafir kaydı: Misafir + Kullanici TEK işlemde oluşturulur.
     * İkincisi başarısız olursa birincisi de geri alınır (yarım kayıt kalmaz).
     */
    @Transactional
    public GirisCevabi kayit(KayitIstegi istek) {
        String eposta = istek.eposta().trim().toLowerCase(Locale.ROOT);

        if (kullaniciRepo.existsByKullaniciAdi(eposta) || misafirRepo.existsByEposta(eposta)) {
            throw ApiHatasi.cakisma("Bu e-posta adresiyle zaten bir hesap var. Giriş yapmayı deneyin.");
        }

        Misafir m = new Misafir();
        m.setAd(istek.ad().trim());
        m.setSoyad(istek.soyad().trim());
        m.setKimlikNo(istek.kimlikNo().trim());
        m.setEposta(eposta);
        m.setTelefon(istek.telefon());
        m.setDogumTarihi(istek.dogumTarihi());
        misafirRepo.save(m);

        Kullanici k = new Kullanici();
        k.setKullaniciAdi(eposta);                              // misafirler e-postayla giriş yapar
        k.setSifreHash(sifreKodlayici.encode(istek.sifre()));   // düz şifre ASLA saklanmaz
        k.setRol(Rol.MISAFIR);
        k.setMisafir(m);
        kullaniciRepo.save(k);

        return cevapOlustur(k);
    }

    @Transactional(readOnly = true)
    public KullaniciBilgisi ben(OturumKullanicisi oturum) {
        Kullanici k = kullaniciRepo.findById(oturum.kullaniciId())
                .orElseThrow(() -> ApiHatasi.yetkisiz("Oturum geçersiz."));
        return bilgi(k);
    }

    // ------------------------------------------------------------ yardımcılar

    private GirisCevabi cevapOlustur(Kullanici k) {
        return new GirisCevabi(jwtServisi.tokenUret(k), "Bearer", jwtServisi.gecerlilikSaniye(), bilgi(k));
    }

    private KullaniciBilgisi bilgi(Kullanici k) {
        String adSoyad = k.getMisafir() != null
                ? k.getMisafir().getAd() + " " + k.getMisafir().getSoyad()
                : k.getPersonel() != null
                    ? k.getPersonel().getAd() + " " + k.getPersonel().getSoyad()
                    : k.getKullaniciAdi();
        return new KullaniciBilgisi(
                k.getId(), k.getKullaniciAdi(), k.getRol(), adSoyad,
                k.getMisafir() != null ? k.getMisafir().getId() : null,
                k.getPersonel() != null ? k.getPersonel().getId() : null);
    }
}