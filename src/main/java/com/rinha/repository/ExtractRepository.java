package com.rinha.repository;

import com.rinha.dto.ExtractDTO;
import com.rinha.dto.TransactionDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.agroal.api.AgroalDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ExtractRepository {

    @Inject
    AgroalDataSource dataSource;

    public ExtractDTO getExtractByClientId(int accountId) {
        String sql = """
            WITH acc AS (
                SELECT id, balance AS total, limite, now() AS data_extrato
                FROM accounts
                WHERE id = ?
            ),
            latest_transactions AS (
                SELECT amount, type, description, created_at
                FROM transactions
                WHERE account_id = (SELECT id FROM acc)
                ORDER BY created_at DESC
                LIMIT 10
            )
            SELECT
                acc.total,
                acc.limite,
                acc.data_extrato,
                (
                    SELECT json_agg(json_build_object(
                                'valor', t.amount,
                                'tipo', t.type,
                                'descricao', t.description,
                                'realizada_em', t.created_at
                            ))
                    FROM latest_transactions t
                ) AS ultimas_transacoes
            FROM acc;
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    BigDecimal limite = rs.getBigDecimal("limite");
                    LocalDateTime dataExtrato = rs.getTimestamp("data_extrato").toLocalDateTime();

                    String json = rs.getString("ultimas_transacoes");
                    List<TransactionDTO> transacoes = (json != null && !json.equals("null"))
                            ? TransactionDTO.fromJsonArray(json)
                            : List.of();

                    ExtractDTO.SaldoDto saldo = new ExtractDTO.SaldoDto(total, limite, dataExtrato);
                    return new ExtractDTO(saldo, transacoes);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro no BD ao buscar extrato: " + e.getMessage(), e);
        }
    }
}
