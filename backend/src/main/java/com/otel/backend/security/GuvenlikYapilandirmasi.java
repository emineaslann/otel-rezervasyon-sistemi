package com.otel.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class GuvenlikYapilandirmasi {

    private final JwtFiltre jwtFiltre;

    @Bean
    public SecurityFilterChain guvenlikZinciri(HttpSecurity http) throws Exception {
        http
            // JWT kullandığımız için oturum (session) ve CSRF koruması gerekmiyor
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(h -> h.disable())

            .authorizeHttpRequests(yetki -> yetki
                // herkese açık uç noktalar
                .requestMatchers(HttpMethod.POST, "/api/auth/giris", "/api/auth/kayit").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/oda-tipleri", "/api/odalar/bos", "/api/ek-hizmetler").permitAll()
                // rol bazlı alanlar
                .requestMatchers("/api/yonetim/**", "/api/raporlar/**").hasRole("YONETICI")
                .requestMatchers("/api/resepsiyon/**", "/api/konaklamalar/**").hasAnyRole("RESEPSIYON", "YONETICI")
                // diğer tüm API'ler: giriş yapmış olmak yeterli (ayrıntı serviste)
                .requestMatchers("/api/**").authenticated()
                // arayüz dosyaları (HTML, CSS, JS)
                .anyRequest().permitAll())

            .exceptionHandling(h -> h
                .authenticationEntryPoint((istek, cevap, e) -> {
                    Object ozelMesaj = istek.getAttribute(JwtFiltre.HATA_OZELLIGI);
                    jsonYaz(cevap, 401, "Unauthorized",
                            ozelMesaj != null ? ozelMesaj.toString() : "Bu işlem için giriş yapmalısınız.");
                })
                .accessDeniedHandler((istek, cevap, e) ->
                    jsonYaz(cevap, 403, "Forbidden", "Bu işlem için yetkiniz yok.")))

            .addFilterBefore(jwtFiltre, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Şifreler BCrypt ile özetlenir (veritabanındaki örnek kullanıcılar da BCrypt). */
    @Bean
    public PasswordEncoder sifreKodlayici() {
        return new BCryptPasswordEncoder();
    }

    /** Arayüz farklı bir adresten (ör. VS Code Live Server) açılırsa da API'ye erişebilsin. */
    @Bean
    public CorsConfigurationSource corsAyarlari() {
        CorsConfiguration ayar = new CorsConfiguration();
        ayar.setAllowedOriginPatterns(List.of("*"));
        ayar.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        ayar.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource kaynak = new UrlBasedCorsConfigurationSource();
        kaynak.registerCorsConfiguration("/api/**", ayar);
        return kaynak;
    }

    /** 401/403 cevaplarını da GlobalHataYakalayici ile aynı JSON biçiminde yazar. */
    private static void jsonYaz(HttpServletResponse cevap, int durum, String hata, String mesaj) throws IOException {
        cevap.setStatus(durum);
        cevap.setContentType("application/json;charset=UTF-8");
        cevap.getWriter().write(String.format(
                "{\"durum\":%d,\"hata\":\"%s\",\"mesaj\":\"%s\",\"alanlar\":null,\"zaman\":\"%s\"}",
                durum, hata, mesaj.replace("\"", "'"), LocalDateTime.now()));
    }
}