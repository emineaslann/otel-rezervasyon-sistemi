package com.otel.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * Saklı prosedür ve görünüm (view) çağrıları için ortak yardımcı.
 * Bir prosedür birden çok sonuç tablosu döndürebilir (ör. sp_KonaklamaToplamTutar 3 tablo);
 * hepsi sırayla okunur. Sütun adları JSON için camelCase'e çevrilir (OdaNo -> odaNo).
 */
@Component
@RequiredArgsConstructor
public class JdbcYardimci {

    private final JdbcTemplate jdbc;

    /** Prosedürü çağırır ve döndürdüğü tüm sonuç tablolarını sırayla verir. */
    public List<List<Map<String, Object>>> prosedurCagir(String prosedur, Object... parametreler) {
        String yerTutucular = String.join(", ", Collections.nCopies(parametreler.length, "?"));
        String sql = "{call " + prosedur + "(" + yerTutucular + ")}";

        return jdbc.execute((ConnectionCallback<List<List<Map<String, Object>>>>) baglanti -> {
            try (CallableStatement cs = baglanti.prepareCall(sql)) {
                for (int i = 0; i < parametreler.length; i++) {
                    cs.setObject(i + 1, parametreler[i]);
                }
                List<List<Map<String, Object>>> sonuclar = new ArrayList<>();
                boolean tabloVar = cs.execute();
                while (true) {
                    if (tabloVar) {
                        try (ResultSet rs = cs.getResultSet()) {
                            sonuclar.add(satirlariOku(rs));
                        }
                    } else if (cs.getUpdateCount() == -1) {
                        break;      // başka sonuç kalmadı
                    }
                    tabloVar = cs.getMoreResults();
                }
                return sonuclar;
            }
        });
    }

    /** Tek tablo döndüren prosedürler için: ilk tablonun ilk satırı. */
    public Map<String, Object> prosedurTekSatir(String prosedur, Object... parametreler) {
        List<List<Map<String, Object>>> sonuc = prosedurCagir(prosedur, parametreler);
        return (sonuc.isEmpty() || sonuc.get(0).isEmpty()) ? Map.of() : sonuc.get(0).get(0);
    }

    /** View veya düz SELECT sorgusu. */
    public List<Map<String, Object>> sorgu(String sql, Object... parametreler) {
        return jdbc.query(sql, (rs, satirNo) -> satiriOku(rs), parametreler);
    }

    // ------------------------------------------------------------ yardımcılar

    private List<Map<String, Object>> satirlariOku(ResultSet rs) throws SQLException {
        List<Map<String, Object>> satirlar = new ArrayList<>();
        while (rs.next()) {
            satirlar.add(satiriOku(rs));
        }
        return satirlar;
    }

    private Map<String, Object> satiriOku(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> satir = new LinkedHashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            Object deger = rs.getObject(i);
            // java.sql tarih tiplerini modern Java tiplerine çevir (JSON'da "2026-10-07" görünsün)
            if (deger instanceof java.sql.Date d) deger = d.toLocalDate();
            else if (deger instanceof Timestamp t) deger = t.toLocalDateTime();
            satir.put(camelCase(md.getColumnLabel(i)), deger);
        }
        return satir;
    }

    /** OdaNo -> odaNo, GeceSayisi -> geceSayisi, ADR -> adr */
    private static String camelCase(String ad) {
        if (ad == null || ad.isEmpty()) return ad;
        if (ad.equals(ad.toUpperCase(Locale.ROOT))) return ad.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(ad.charAt(0)) + ad.substring(1);
    }
}