package com.rinha.dto.transaction;

public record TransactionRequestDTO (
    int valor,
    char tipo,
    String descricao
) {}

