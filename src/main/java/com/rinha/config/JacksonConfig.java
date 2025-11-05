package com.rinha.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        // Evita erro se vier campo a mais no JSON
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Aceita floats em campos inteiros
        mapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);

        // Formata datas legíveis (ex: "2025-11-05T10:30:00")
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Define o padrão de nomenclatura (snake_case ou camelCase)
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    }
}
