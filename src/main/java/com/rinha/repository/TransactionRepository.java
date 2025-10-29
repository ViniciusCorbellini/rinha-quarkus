package com.rinha.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.exceptions.EntityNotFoundException;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.InternalServerErrorException;

@ApplicationScoped
public class TransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    @Transactional
    public TransactionResponseDTO processTransaction(int clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, EntityNotFoundException, RuntimeException {

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT novo_limite, novo_saldo FROM realizar_transacao(?, ?, ?, ?);";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, clientId);
            ps.setBigDecimal(2, dto.getValor());
            ps.setString(3, dto.getTipo());
            ps.setString(4, dto.getDescricao());

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new EntityNotFoundException("Cliente não encontrado");
            }

            return new TransactionResponseDTO(
                    rs.getBigDecimal("novo_limite"),
                    rs.getBigDecimal("novo_saldo"));

        } catch (SQLException e) {
            String causeMessage = e.getMessage();

            if (causeMessage == null) {
                throw new RuntimeException("Erro inesperado ao processar a transação.", e);
            }

            if (causeMessage.contains("CLIENTE_NAO_ENCONTRADO")) {
                throw new EntityNotFoundException(causeMessage);
            }

            if (causeMessage.contains("LIMITE_INDISPONIVEL")) {
                throw new IllegalArgumentException(causeMessage);
            }
        } catch(Exception e){
            throw new InternalServerErrorException("Erro no BD");
        }
        return null;
    }
}
