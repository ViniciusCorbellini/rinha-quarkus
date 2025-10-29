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

        String descricao = dto.getDescricao();
        if (descricao == null || descricao.isBlank() || descricao.length() > 10) {
            throw new IllegalArgumentException("Descricao invalida");
        }

        // --- TODO ---
        // Comparação de 'char' é ordens de magnitude mais rápida
        // que String.equals()
        char tipo = dto.getTipo();
        if ('c' != tipo && 'd' != tipo) {
            throw new IllegalArgumentException("Tipo invalido");
        }

        int valor = dto.getValor();
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor invalido");
        }

        return repository.processTransaction(clientId, dto);
    }
}
