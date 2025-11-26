package com.rinha.service;

import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.repository.TransactionRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionService {

    @Inject
    TransactionRepository repository;

    public TransactionResponseDTO processTransaction(int clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, RuntimeException {

        if (clientId <= 0) {
            throw new IllegalArgumentException("Id invalido");
        }

        String descricao = dto.descricao();
        if (descricao == null || descricao.isBlank() || descricao.length() > 10) {
            throw new IllegalArgumentException("Descricao invalida");
        }

        char tipo = dto.tipo();
        if ('c' != tipo && 'd' != tipo) {
            throw new IllegalArgumentException("Tipo invalido");
        }

        int valor = dto.valor();
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor invalido");
        }

        return repository.createTransaction(clientId, dto);
    }
}
