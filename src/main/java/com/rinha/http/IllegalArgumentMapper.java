package com.rinha.http;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException ex) {
        // 422 cobre "semântica inválida" (tipo, descricao, valor<=0 etc.)
        return Response.status(422)
                .entity(ex.getMessage()) // opcional: pode omitir o corpo
                .build();
    }
}