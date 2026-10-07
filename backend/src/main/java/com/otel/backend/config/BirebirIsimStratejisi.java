package com.otel.backend.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Tablo ve sütun adlarını HİÇ DEĞİŞTİRMEDEN kullanır.
 * (Spring'in varsayılanı OdaTipi -> oda_tipi yapar; bizim tablolarımız OdaTipi.)
 */
public class BirebirIsimStratejisi implements PhysicalNamingStrategy {

    public Identifier toPhysicalCatalogName(Identifier ad, JdbcEnvironment ortam) { return ad; }

    public Identifier toPhysicalSchemaName(Identifier ad, JdbcEnvironment ortam) { return ad; }

    public Identifier toPhysicalTableName(Identifier ad, JdbcEnvironment ortam) { return ad; }

    public Identifier toPhysicalSequenceName(Identifier ad, JdbcEnvironment ortam) { return ad; }

    public Identifier toPhysicalColumnName(Identifier ad, JdbcEnvironment ortam) { return ad; }
}