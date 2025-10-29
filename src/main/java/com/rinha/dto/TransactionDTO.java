package com.rinha.dto;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TransactionDTO(
        int valor,
        char tipo,
        String descricao,
        LocalDateTime realizada_em
) {}
