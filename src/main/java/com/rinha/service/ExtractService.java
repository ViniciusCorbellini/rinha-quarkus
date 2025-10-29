package com.rinha.service;

import com.rinha.dto.ExtractDTO;
import com.rinha.exceptions.EntityNotFoundException;
import com.rinha.repository.ExtractRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExtractService {

    @Inject
    ExtractRepository repository;

    public ExtractDTO getExtract(int clientId)
            throws EntityNotFoundException, IllegalArgumentException {

        if (clientId <= 0) {
            throw new IllegalArgumentException("Id invalido");
        }

        // --- OTIMIZAÇÃO (REMOÇÃO) ---
        // O repository.getExtractByClientId(clientId) agora lança
        // EntityNotFoundException se o cliente não existir.
        // O 'if (extract == null)' foi removido.
        // Esta chamada ou retorna o Extrato ou lança a Exceção.
        // Não há mais 'null' para checar.
        return repository.getExtractByClientId(clientId);
    }
}
