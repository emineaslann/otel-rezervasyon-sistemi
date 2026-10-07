package com.otel.backend.security;

import com.otel.backend.entity.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Her istekte bir kez çalışır: "Authorization: Bearer <token>" başlığını okur,
 * token geçerliyse kullanıcıyı ve rolünü Spring Security'ye tanıtır.
 */
@Component
@RequiredArgsConstructor
public class JwtFiltre extends OncePerRequestFilter {

    public static final String HATA_OZELLIGI = "jwtHata";

    private final JwtServisi jwtServisi;

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse cevap, FilterChain zincir)
            throws ServletException, IOException {

        String baslik = istek.getHeader("Authorization");
        if (baslik != null && baslik.startsWith("Bearer ")) {
            String token = baslik.substring(7);
            try {
                Claims c = jwtServisi.dogrula(token);
                OturumKullanicisi kullanici = new OturumKullanicisi(
                        Integer.valueOf(c.getSubject()),
                        c.get("kullaniciAdi", String.class),
                        Rol.valueOf(c.get("rol", String.class)),
                        c.get("misafirId", Integer.class),
                        c.get("personelId", Integer.class));

                // Spring Security rolleri "ROLE_" önekiyle bekler: ROLE_YONETICI
                var yetkiler = List.of(new SimpleGrantedAuthority("ROLE_" + kullanici.rol().name()));
                var kimlik = new UsernamePasswordAuthenticationToken(kullanici, null, yetkiler);
                SecurityContextHolder.getContext().setAuthentication(kimlik);

            } catch (ExpiredJwtException e) {
                istek.setAttribute(HATA_OZELLIGI, "Oturumunuzun süresi doldu, lütfen tekrar giriş yapın.");
            } catch (JwtException | IllegalArgumentException e) {
                istek.setAttribute(HATA_OZELLIGI, "Oturum bilgisi geçersiz, lütfen tekrar giriş yapın.");
            }
        }
        zincir.doFilter(istek, cevap);
    }
}