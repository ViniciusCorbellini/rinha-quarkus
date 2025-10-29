package com.rinha.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ExtractDTO(
        SaldoDto saldo,
        List<TransactionDTO> ultimas_transacoes
) {
    public record SaldoDto(
            int total,
            int limite,
            LocalDateTime data_extrato
    ) {}
}
