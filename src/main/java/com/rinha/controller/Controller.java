package com.rinha.controller;

import com.rinha.dto.ExtractDTO;
import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.exceptions.EntityNotFoundException;
import com.rinha.service.ExtractService;
import com.rinha.service.TransactionService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Controller {

    @Inject
    ExtractService extractService;

    @Inject
    TransactionService transactionService;

    @GET
    @Path("/{id}/extrato")
    public Response getExtrato(@PathParam("id") Integer id) {
        try {
            ExtractDTO extract = extractService.getExtract(id);
            return Response.ok(extract).build();
        } catch (EntityNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Client not found\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal Error\"}")
                    .build();
        }
    }

    @POST
    @Path("/{id}/transacoes")
    public Response createTransaction(@PathParam("id") Integer id, TransactionRequestDTO dto) {
        try {
            TransactionResponseDTO response = transactionService.processTransaction(id, dto);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422)
                    .entity("{\"erro\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (EntityNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"erro\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"erro\": \"Internal Error\"}")
                    .build();
        }
    }
}

