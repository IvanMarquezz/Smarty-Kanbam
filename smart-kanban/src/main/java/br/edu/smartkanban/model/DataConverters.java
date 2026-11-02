package br.edu.smartkanban.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Conversores que gravam datas como texto ISO-8601 (ex.: "2026-11-30") no SQLite.
 * O SQLite não possui tipo de data nativo; guardar texto ISO é simples, legível e ordenável.
 */
public final class DataConverters {
    private DataConverters() { }

    @Converter
    public static class LocalDateConverter implements AttributeConverter<LocalDate, String> {
        @Override public String convertToDatabaseColumn(LocalDate d) { return d == null ? null : d.toString(); }
        @Override public LocalDate convertToEntityAttribute(String s) { return s == null ? null : LocalDate.parse(s); }
    }

    @Converter
    public static class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {
        @Override public String convertToDatabaseColumn(LocalDateTime d) { return d == null ? null : d.toString(); }
        @Override public LocalDateTime convertToEntityAttribute(String s) { return s == null ? null : LocalDateTime.parse(s); }
    }
}
