package com.rinha.exceptions;

import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException ex) {
        return Response.status(422)
                .entity("Erro de semântica no JSON: " + ex.getOriginalMessage())
                .build();
    }
}
