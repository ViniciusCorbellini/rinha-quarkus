package com.rinha.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.rinha.dto.ExtractDTO;
import com.rinha.dto.TransactionDTO;
import com.rinha.exceptions.EntityNotFoundException;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExtractRepository {

    @Inject
    AgroalDataSource dataSource;

    public ExtractDTO getExtractByClientId(int accountId) {
        // Chamar a nova função "flat"
        String sql = "SELECT * FROM obter_extrato(?);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            
            try (ResultSet rs = ps.executeQuery()) {
                ExtractDTO.SaldoDto saldo = null;
                List<TransactionDTO> transacoes = new ArrayList<>(10); // Pré-alocar espaço

                boolean clientExists = false;

                while (rs.next()) {
                    clientExists = true;

                    // Otimização: ler o saldo apenas na primeira linha
                    if (saldo == null) {
                        saldo = new ExtractDTO.SaldoDto(
                            rs.getInt("total"),
                            rs.getInt("limite"),
                            rs.getTimestamp("data_extrato").toLocalDateTime()
                        );
                    }

                    // Checar se a transação existe (pode ser nula devido ao LEFT JOIN)
                    Timestamp transTimestamp = rs.getTimestamp("trans_realizada_em");
                    if (transTimestamp != null) {
                        transacoes.add(new TransactionDTO(
                            rs.getInt("trans_valor"),
                            rs.getString("trans_tipo").charAt(0),
                            rs.getString("trans_descricao"),
                            transTimestamp.toLocalDateTime()
                        ));
                    }
                }

                if (!clientExists) {
                    throw new EntityNotFoundException("Cliente não encontrado: " + accountId);
                }

                // Se o cliente existe mas não tem transações, o 'saldo' foi lido
                // e a lista 'transacoes' estará vazia.
                return new ExtractDTO(saldo, transacoes);
                
            }
        } catch (SQLException e) {
            // Adicionar tratamento de SQLSTATE se o DB lançar erro de "não encontrado"
            throw new RuntimeException("Erro no BD ao buscar extrato: " + e.getMessage(), e);
        }
    }
}