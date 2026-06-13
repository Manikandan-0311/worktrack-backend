package com.spearhead.ufc.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.io.IOException;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMAT));

        // Custom deserializer: try ISO first, then dd/MM/yyyy
        JsonDeserializer<LocalDate> multiDeserializer = new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                if (text == null || text.isEmpty()) return null;
                // try ISO
                try {
                    return LocalDate.parse(text);
                } catch (DateTimeParseException ignored) {
                }
                // try configured pattern
                try {
                    return LocalDate.parse(text, DATE_FORMAT);
                } catch (DateTimeParseException ex) {
                    throw InvalidFormatException.from(p, "Cannot deserialize LocalDate", text, LocalDate.class);
                }
            }
        };

        module.addDeserializer(LocalDate.class, multiDeserializer);
        builder.modulesToInstall(module);
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return builder;
    }
}
