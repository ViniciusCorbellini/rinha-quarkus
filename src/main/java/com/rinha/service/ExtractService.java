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

    public ExtractDTO getExtract(Integer clientId) throws EntityNotFoundException, IllegalArgumentException {
        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("Id invalido");
        }

        ExtractDTO extract = repository.getExtractByClientId(clientId);

        if (extract == null) {
            throw new EntityNotFoundException("Client not found");
        }

        return extract;
    }
}
