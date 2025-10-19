package com.rinha.service;

import java.math.BigDecimal;

import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.repository.TransactionRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionService {

    @Inject
    TransactionRepository repository;

    public TransactionResponseDTO processTransaction(Integer clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, RuntimeException {

        if (clientId == null || clientId <= 0)
            throw new IllegalArgumentException("Id invalido");

        String descricao = dto.getDescricao();
        if (descricao == null || descricao.isBlank() || descricao.length() > 10) {
            throw new IllegalArgumentException("Descricao invalida");
        }

        String tipo = dto.getTipo();
        if (!"c".equals(tipo) && !"d".equals(tipo)) {
            throw new IllegalArgumentException("Tipo invalido");
        }

        BigDecimal valor = dto.getValor();
        if (valor == null || valor.signum() <= 0 || valor.scale() > 0) {
            throw new IllegalArgumentException("Valor invalido");
        }

        return repository.processTransaction(clientId, dto);
    }
}
