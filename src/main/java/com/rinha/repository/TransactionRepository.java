package com.rinha.repository;

import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.exceptions.EntityNotFoundException;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class TransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    @Transactional
    public TransactionResponseDTO processTransaction(int clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, EntityNotFoundException, RuntimeException {

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT limite, novo_saldo FROM realizar_transacao(?, ?, ?, ?);";

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
                    rs.getBigDecimal("limite"),
                    rs.getBigDecimal("balance"));

        } catch (SQLException e) {
            String causeMessage = e.getMessage();

            if (causeMessage == null) {
                throw new RuntimeException("Erro inesperado ao processar a transação.", e);
            }

            if (causeMessage.contains("Cliente não encontrado")) {
                throw new EntityNotFoundException(causeMessage);
            }

            if (causeMessage.contains("Limite da conta excedido")) {
                throw new IllegalArgumentException(causeMessage);
            }
        }
        return null;
    }
}
