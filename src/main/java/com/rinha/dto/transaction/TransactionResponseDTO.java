package com.rinha.dto.transaction;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionResponseDTO {

    private int limite;
    private int saldo;

    public TransactionResponseDTO(int limite, int saldo) {
        this.limite = limite;
        this.saldo = saldo;
    }

    public int getLimite() {
        return limite;
    }

    public int getSaldo() {
        return saldo;
    }
}

