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

@ApplicationScoped
public class TransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    public TransactionResponseDTO createTransaction(int accountId, TransactionRequestDTO req) {
        if (accountId < 1 || accountId > 5) {
            throw new EntityNotFoundException("Cliente não encontrado");
        }

        String sql = "SELECT process_transaction(?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ps.setInt(2, req.valor());
            ps.setString(3, String.valueOf(req.tipo()));
            ps.setString(4, req.descricao());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String jsonResult = rs.getString(1);

                    // Validação de Erro de Negócio (Regra para HTTP 422)
                    // Se o ID já passou na validação acima (1-5), e o banco retornou erro,
                    // a única causa possível é falta de saldo/limite.
                    if (jsonResult == null || jsonResult.contains("\"error\":")) {
                        throw new IllegalArgumentException("Limite excedido ou saldo insuficiente");
                    }

                    return parseJsonToDto(jsonResult);
                }
            }
        } catch (SQLException e) {
            // Capturado pelo catch(Exception e) -> 500 Internal Server Error
            throw new RuntimeException("Erro de conexão ao processar transação", e);
        }

        // Caso defensivo (não deve acontecer com a function atual)
        throw new RuntimeException("Erro inesperado: banco não retornou resposta");
    }

    /**
     * Faz o parsing manual da string JSON {"saldo": X, "limite": Y} Evita o
     * overhead do Jackson/Gson para essa estrutura simples e crítica.
     */
    private TransactionResponseDTO parseJsonToDto(String json) {
        // Remove chaves e aspas para facilitar
        // json original: {"saldo": -900, "limite": 1000}
        // "limpo": saldo: -900, limite: 1000

        String content = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        String[] parts = content.split(",");

        int saldo = 0;
        int limite = 0;

        for (String part : parts) {
            String[] kv = part.split(":");
            String key = kv[0].trim();
            int value = Integer.parseInt(kv[1].trim());

            if ("saldo".equals(key)) {
                saldo = value;
            } else if ("limite".equals(key)) {
                limite = value;
            }
        }

        return new TransactionResponseDTO(limite, saldo);
    }
}
