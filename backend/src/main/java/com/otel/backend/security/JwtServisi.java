package com.otel.backend.security;

import com.otel.backend.entity.Kullanici;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** JWT üretme ve doğrulama. İmza algoritması: HMAC-SHA256. */
@Service
public class JwtServisi {

    private final SecretKey anahtar;
    private final Duration gecerlilik;

    public JwtServisi(@Value("${app.jwt.gizli-anahtar}") String gizliAnahtar,
                      @Value("${app.jwt.gecerlilik-saat:8}") long saat) {
        byte[] baytlar = gizliAnahtar.getBytes(StandardCharsets.UTF_8);
        if (baytlar.length < 32) {
            throw new IllegalStateException("app.jwt.gizli-anahtar en az 32 karakter olmalı.");
        }
        this.anahtar = Keys.hmacShaKeyFor(baytlar);
        this.gecerlilik = Duration.ofHours(saat);
    }

    /** Giriş yapan kullanıcı için imzalı token üretir. */
    public String tokenUret(Kullanici k) {
        Instant simdi = Instant.now();
        JwtBuilder token = Jwts.builder()
                .subject(String.valueOf(k.getId()))
                .claim("kullaniciAdi", k.getKullaniciAdi())
                .claim("rol", k.getRol().name())
                .issuedAt(Date.from(simdi))
                .expiration(Date.from(simdi.plus(gecerlilik)));

        if (k.getMisafir() != null)  token.claim("misafirId", k.getMisafir().getId());
        if (k.getPersonel() != null) token.claim("personelId", k.getPersonel().getId());

        return token.signWith(anahtar).compact();
    }

    /**
     * Token'ın imzasını ve süresini kontrol eder, içeriğini döndürür.
     * İmza bozuksa veya süresi dolmuşsa JwtException fırlatır.
     */
    public Claims dogrula(String token) {
        return Jwts.parser()
                .verifyWith(anahtar)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long gecerlilikSaniye() {
        return gecerlilik.toSeconds();
    }
}