package com.otel.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Servis katmanının fırlattığı, HTTP durum koduyla birlikte gelen hata. */
@Getter
public class ApiHatasi extends RuntimeException {

    private final HttpStatus durum;

    public ApiHatasi(HttpStatus durum, String mesaj) {
        super(mesaj);
        this.durum = durum;
    }

    public static ApiHatasi bulunamadi(String mesaj) { return new ApiHatasi(HttpStatus.NOT_FOUND, mesaj); }
    public static ApiHatasi gecersiz(String mesaj)   { return new ApiHatasi(HttpStatus.BAD_REQUEST, mesaj); }
    public static ApiHatasi yetkisiz(String mesaj)   { return new ApiHatasi(HttpStatus.UNAUTHORIZED, mesaj); }
    public static ApiHatasi yasak(String mesaj)      { return new ApiHatasi(HttpStatus.FORBIDDEN, mesaj); }
    public static ApiHatasi cakisma(String mesaj)    { return new ApiHatasi(HttpStatus.CONFLICT, mesaj); }
}