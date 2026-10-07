package com.otel.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uygulamadaki TÜM hataları tek yerde yakalar ve anlaşılır JSON cevaplarına çevirir.
 * MySQL hataları (trigger/prosedür SIGNAL, UNIQUE, FK, CHECK) hata koduna göre ayrıştırılır.
 */
@Slf4j
@RestControllerAdvice
public class GlobalHataYakalayici {

    /** UNIQUE kısıt adı -> kullanıcı mesajı */
    private static final Map<String, String> TEKRAR_MESAJLARI = Map.of(
            "uq_misafir_eposta", "Bu e-posta adresiyle kayıtlı bir misafir zaten var.",
            "uq_misafir_kimlik", "Bu kimlik/pasaport numarasıyla kayıtlı bir misafir zaten var.",
            "uq_kullanici_ad", "Bu kullanıcı adı zaten kullanılıyor.",
            "uq_oda_no", "Bu oda numarası zaten tanımlı.",
            "uq_odatipi_ad", "Bu oda tipi adı zaten tanımlı.",
            "uq_ekhizmet_ad", "Bu hizmet adı zaten tanımlı.",
            "uq_konaklama_rez", "Bu rezervasyon için zaten check-in yapılmış."
    );

    /** CHECK kısıt adı -> kullanıcı mesajı */
    private static final Map<String, String> KURAL_MESAJLARI = Map.ofEntries(
            Map.entry("ck_rez_tarih", "Çıkış tarihi giriş tarihinden sonra olmalı."),
            Map.entry("ck_rez_kisi", "Kişi sayısı en az 1 olmalı."),
            Map.entry("ck_odeme_tutar", "Ödeme tutarı sıfırdan büyük olmalı."),
            Map.entry("ck_sezon_tarih", "Sezon bitiş tarihi başlangıçtan önce olamaz."),
            Map.entry("ck_sezon_fiyat", "Gecelik fiyat sıfırdan büyük olmalı."),
            Map.entry("ck_odatipi_fiyat", "Temel fiyat sıfırdan büyük olmalı."),
            Map.entry("ck_odatipi_kapasite", "Kapasite 1 ile 10 arasında olmalı."),
            Map.entry("ck_misafir_eposta", "E-posta adresi geçerli görünmüyor."),
            Map.entry("ck_kh_adet", "Adet en az 1 olmalı."),
            Map.entry("ck_ekhizmet_fiyat", "Birim fiyat negatif olamaz."),
            Map.entry("ck_oda_kat", "Kat -2 ile 50 arasında olmalı."),
            Map.entry("ck_kullanici_baglanti", "Kullanıcı hesabı bir misafire veya personele bağlı olmalı.")
    );

    // ------------------------------------------------------------ uygulama hataları

    @ExceptionHandler(ApiHatasi.class)
    public ResponseEntity<HataCevabi> apiHatasi(ApiHatasi e) {
        return cevap(e.getDurum(), e.getMessage());
    }

    // ------------------------------------------------------------ veritabanı hataları

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<HataCevabi> veritabani(DataAccessException e) {
        Throwable kok = NestedExceptionUtils.getMostSpecificCause(e);
        if (kok instanceof SQLException sql) {
            String mesaj = sql.getMessage();
            switch (sql.getErrorCode()) {
                case 1644:  // SIGNAL SQLSTATE '45000' -> trigger veya prosedürdeki iş kuralı
                    return cevap(HttpStatus.CONFLICT, mesaj);
                case 1062:  // UNIQUE ihlali
                    return cevap(HttpStatus.CONFLICT,
                            TEKRAR_MESAJLARI.getOrDefault(kisitAdi(mesaj, "for key '(?:\\w+\\.)?(\\w+)'"),
                                    "Bu kayıt zaten mevcut."));
                case 1451:  // silinmek istenen kayda başka kayıtlar bağlı
                    return cevap(HttpStatus.CONFLICT,
                            "Bu kayıt başka kayıtlarda kullanıldığı için silinemez. Bunun yerine pasif hale getirebilirsiniz.");
                case 1452:  // olmayan bir kayda bağlanmaya çalışıldı
                    return cevap(HttpStatus.BAD_REQUEST, "Seçilen ilişkili kayıt bulunamadı.");
                case 3819:  // CHECK kısıtı
                    String ad = kisitAdi(mesaj, "constraint '(\\w+)'");
                    return cevap(HttpStatus.BAD_REQUEST,
                            KURAL_MESAJLARI.getOrDefault(ad, "Veri kuralı ihlal edildi (" + ad + ")."));
                case 1265: case 1366: case 1292: case 1264:  // ENUM dışı değer, hatalı tarih vb.
                    return cevap(HttpStatus.BAD_REQUEST, "Gönderilen değerlerden biri geçersiz.");
                case 1048:
                    return cevap(HttpStatus.BAD_REQUEST, "Zorunlu bir alan boş bırakılmış.");
                default:
                    break;
            }
        }
        log.error("Beklenmeyen veritabanı hatası", e);
        return cevap(HttpStatus.INTERNAL_SERVER_ERROR, "Veritabanı hatası oluştu.");
    }

    // ------------------------------------------------------------ istek hataları

    /** @Valid ile işaretli istek gövdesindeki alan hataları */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HataCevabi> dogrulama(MethodArgumentNotValidException e) {
        Map<String, String> alanlar = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(h -> alanlar.putIfAbsent(h.getField(), h.getDefaultMessage()));
        HataCevabi govde = new HataCevabi(400, "Bad Request",
                "Gönderilen bilgilerde hata var.", alanlar, LocalDateTime.now());
        return ResponseEntity.badRequest().body(govde);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HataCevabi> okunamayanGovde(HttpMessageNotReadableException e) {
        return cevap(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı. JSON biçimini ve değerleri kontrol edin.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<HataCevabi> parametre(Exception e) {
        return cevap(HttpStatus.BAD_REQUEST, "Adres parametrelerinden biri eksik veya hatalı.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HataCevabi> erisimYok(AccessDeniedException e) {
        return cevap(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HataCevabi> adresYok(NoResourceFoundException e) {
        return cevap(HttpStatus.NOT_FOUND, "İstenen adres bulunamadı.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HataCevabi> beklenmeyen(Exception e) {
        log.error("Beklenmeyen hata", e);
        return cevap(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu.");
    }

    // ------------------------------------------------------------ yardımcılar

    private static ResponseEntity<HataCevabi> cevap(HttpStatus durum, String mesaj) {
        return ResponseEntity.status(durum).body(HataCevabi.of(durum.value(), durum.getReasonPhrase(), mesaj));
    }

    private static String kisitAdi(String mesaj, String desen) {
        Matcher m = Pattern.compile(desen).matcher(mesaj == null ? "" : mesaj);
        return m.find() ? m.group(1) : "";
    }
}