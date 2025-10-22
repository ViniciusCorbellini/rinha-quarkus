package com.rinha.dto.transaction;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionResponseDTO {

    private BigDecimal limite;
    private BigDecimal saldo;

    public TransactionResponseDTO(BigDecimal limite, BigDecimal saldo) {
        this.limite = limite;
        this.saldo = saldo;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}

