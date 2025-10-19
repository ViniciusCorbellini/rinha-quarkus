package com.rinha.repository;

import com.rinha.dto.account.AccountData;
import com.rinha.dto.transaction.TransactionRequestDTO;
import com.rinha.dto.transaction.TransactionResponseDTO;
import com.rinha.exceptions.EntityNotFoundException;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@ApplicationScoped
public class TransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    @Transactional
    public TransactionResponseDTO processTransaction(int clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, EntityNotFoundException {

        try (Connection conn = dataSource.getConnection()) {
            String sqlSelect = "SELECT limite, balance FROM accounts WHERE id = ? FOR UPDATE";
            AccountData account;

            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setInt(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new EntityNotFoundException("Cliente não encontrado");
                    }
                    account = new AccountData(
                            rs.getBigDecimal("limite"),
                            rs.getBigDecimal("balance")
                    );
                }
            }

            BigDecimal accountLimit = account.limite();
            BigDecimal balance = account.balance();
            BigDecimal newBalance = balance;

            if ("c".equals(dto.getTipo())) {
                newBalance = newBalance.add(dto.getValor());
            } else {
                newBalance = newBalance.subtract(dto.getValor());
                if (newBalance.compareTo(accountLimit.negate()) < 0) {
                    throw new IllegalArgumentException("Limite excedido");
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE accounts SET balance = ? WHERE id = ?")) {
                psUpdate.setBigDecimal(1, newBalance);
                psUpdate.setInt(2, clientId);
                psUpdate.executeUpdate();
            }

            try (PreparedStatement psInsert = conn.prepareStatement(
                    "INSERT INTO transactions (amount, type, description, account_id) VALUES (?, ?, ?, ?)")) {
                psInsert.setBigDecimal(1, dto.getValor());
                psInsert.setString(2, dto.getTipo());
                psInsert.setString(3, dto.getDescricao());
                psInsert.setInt(4, clientId);
                psInsert.executeUpdate();
            }

            return new TransactionResponseDTO(accountLimit, newBalance);

        } catch (EntityNotFoundException | IllegalArgumentException e) {
            throw e; // Propaga as exceções conhecidas
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar transação: " + e.getMessage(), e);
        }
    }
}
