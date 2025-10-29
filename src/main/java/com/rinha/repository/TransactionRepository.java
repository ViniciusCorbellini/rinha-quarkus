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
import jakarta.ws.rs.InternalServerErrorException;

@ApplicationScoped
public class TransactionRepository {

    @Inject
    AgroalDataSource dataSource;

    // Códigos SQLSTATE customizados, definidos no init.sql
    private static final String ERR_CLIENTE_NAO_ENCONTRADO = "P0001";
    private static final String ERR_LIMITE_INDISPONIVEL = "P0002";
    // Código SQLSTATE padrão do Postgres para CHECK constraint
    private static final String ERR_CHECK_VIOLATION = "23514";

    public TransactionResponseDTO processTransaction(int clientId, TransactionRequestDTO dto)
            throws IllegalArgumentException, EntityNotFoundException, RuntimeException {

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT novo_saldo, novo_limite FROM realizar_transacao(?, ?, ?, ?);";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, clientId);
            ps.setInt(2, dto.getValor());
            ps.setString(3, String.valueOf(dto.getTipo()));
            ps.setString(4, dto.getDescricao());

            try (ResultSet rs = ps.executeQuery()) {
                // Se o DB foi alterado para lançar exceção, rs.next()
                // NUNCA será falso. Ele só é chamado se a query for um sucesso.
                // Isso simplifica o "happy path".
                if (!rs.next()) {
                    // Este bloco SÓ será atingido se a função do DB retornar 0 linhas
                    // em vez de lançar uma exceção P0001.
                    // Mantenha apenas UMA estratégia (exceção é melhor).
                    // Assumindo que o DB lança exceção, esta linha abaixo é desnecessária.
                    throw new EntityNotFoundException("Cliente não encontrado");

                }

                return new TransactionResponseDTO(
                        rs.getInt("novo_limite"),
                        rs.getInt("novo_saldo"));

            }

        } catch (SQLException e) {
            String sqlState = e.getSQLState();

            if (sqlState == null) {
                // Erro não esperado, sem SQLSTATE
                throw new InternalServerErrorException("Erro inesperado de SQL.", e);
            }

            // Esta é a otimização. Um switch em 5 caracteres.
            // É ordens de magnitude mais rápido que .getMessage().contains()
            switch (sqlState) {
                case ERR_CLIENTE_NAO_ENCONTRADO: // "P0001"
                    throw new EntityNotFoundException("Cliente não encontrado: " + clientId);

                case ERR_LIMITE_INDISPONIVEL:  // "P0002"
                case ERR_CHECK_VIOLATION:      // "23514"
                    throw new IllegalArgumentException("Limite indisponível.");

                default:
                    // Outro erro de SQL que não mapeamos (ex: '23503' FK violation)
                    throw new InternalServerErrorException("Erro no BD ao processar transação.", e);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Erro ao processar resposta do BD.", e);
        }
    }
}
