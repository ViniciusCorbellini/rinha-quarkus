package com.rinha.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
        // 1. Fail Fast: Validação de ID Estática (Regra da Rinha: IDs 1 a 5)
        if (accountId < 1 || accountId > 5) {
            throw new EntityNotFoundException("Cliente não encontrado");
        }

        // A função retorna um JSON único ou NULL
        String sql = "SELECT get_extrato(?)";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                // Se não retornar linha ou retornar NULL na primeira coluna
                if (!rs.next()) {
                    throw new EntityNotFoundException("Cliente não encontrado");
                }

                String jsonResult = rs.getString(1);
                if (jsonResult == null) {
                    // A função SQL retorna NULL se o ID não existe na tabela accounts
                    throw new EntityNotFoundException("Cliente não encontrado");
                }

                // Parsing manual otimizado para a estrutura fixa do Postgres
                return parseJsonToExtractDTO(jsonResult);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro no BD ao buscar extrato", e);
        }
    }

    private ExtractDTO parseJsonToExtractDTO(String json) {
        try {
            int total = extractInt(json, "\"total\"");
            int limite = extractInt(json, "\"limite\"");

            // CORREÇÃO 1: Parse de data com Fuso Horário
            String dataStr = extractString(json, "\"data_extrato\"");
            // Remove aspas e garante o formato ISO
            dataStr = dataStr.replace("\"", "").replace(" ", "T");

            // OffsetDateTime recebe o "+00:00" sem reclamar
            LocalDateTime dataExtrato = OffsetDateTime.parse(dataStr).toLocalDateTime();

            ExtractDTO.SaldoDto saldoDto = new ExtractDTO.SaldoDto(total, limite, dataExtrato);

            List<TransactionDTO> transacoes = new ArrayList<>();
            int idxTransacoes = json.indexOf("\"ultimas_transacoes\"");
            int idxArrayStart = json.indexOf("[", idxTransacoes);
            int idxArrayEnd = json.lastIndexOf("]");

            if (idxArrayStart != -1 && idxArrayEnd != -1) {
                String arrayContent = json.substring(idxArrayStart + 1, idxArrayEnd).trim();
                if (!arrayContent.isEmpty()) {
                    String[] transObjs = arrayContent.split("\\},\\s*\\{");
                    for (String tJson : transObjs) {
                        transacoes.add(parseTransactionJson(tJson.replace("{", "").replace("}", "")));
                    }
                }
            }
            return new ExtractDTO(saldoDto, transacoes);

        } catch (Exception e) {
            // Log de erro reduzido para não poluir, mas útil
            System.err.println("JSON Parse Error: " + e.getMessage());
            throw new RuntimeException("Erro ao processar JSON", e);
        }
    }

    private TransactionDTO parseTransactionJson(String jsonContent) {
        int valor = extractInt(jsonContent, "\"valor\"");

        String tipoStr = extractString(jsonContent, "\"tipo\"");
        char tipo = tipoStr.isEmpty() ? ' ' : tipoStr.replace("\"", "").trim().charAt(0);

        String descricao = extractString(jsonContent, "\"descricao\"");

        // CORREÇÃO 2: Tratamento de NULL e Data
        String dataStr = extractString(jsonContent, "\"realizada_em\""); // ou "created_at"
        LocalDateTime realizadaEm;

        // Se vier "null" (string) ou vazio, usamos Agora para não quebrar a aplicação
        if (dataStr == null || dataStr.isEmpty() || dataStr.equals("null") || dataStr.equals("\"null\"")) {
            realizadaEm = LocalDateTime.now();
        } else {
            dataStr = dataStr.replace("\"", "").replace(" ", "T");
            realizadaEm = OffsetDateTime.parse(dataStr).toLocalDateTime();
        }

        return new TransactionDTO(valor, tipo, descricao, realizadaEm);
    }

    // --- MÉTODOS AUXILIARES ROBUSTOS ---
    /**
     * Busca um inteiro numa string JSON de forma tolerante a espaços. Ex:
     * procura "chave" ... : ... 123 ... ,
     */
    private int extractInt(String source, String key) {
        int keyIdx = source.indexOf(key);
        if (keyIdx == -1) {
            return 0; // Ou lançar erro
        }
        // A partir da chave, acha os dois pontos ":"
        int colonIdx = source.indexOf(":", keyIdx);

        // O valor termina na vírgula "," ou no fim do objeto "}"
        int commaIdx = source.indexOf(",", colonIdx);
        int braceIdx = source.indexOf("}", colonIdx);

        // Pega o que vier primeiro (vírgula ou chave fechando)
        int endIdx;
        if (commaIdx == -1) {
            endIdx = braceIdx;
        } else if (braceIdx == -1) {
            endIdx = commaIdx;
        } else {
            endIdx = Math.min(commaIdx, braceIdx);
        }

        if (endIdx == -1) {
            endIdx = source.length(); // Fallback
        }
        String value = source.substring(colonIdx + 1, endIdx).trim();
        return Integer.parseInt(value);
    }

    /**
     * Busca uma string (entre aspas) numa string JSON.
     */
    private String extractString(String source, String key) {
        int keyIdx = source.indexOf(key);
        if (keyIdx == -1) {
            return "";
        }

        int colonIdx = source.indexOf(":", keyIdx);

        // Procura a primeira aspas DEPOIS dos dois pontos
        int firstQuote = source.indexOf("\"", colonIdx);

        // Procura a aspas de fechamento
        int secondQuote = source.indexOf("\"", firstQuote + 1);

        if (firstQuote != -1 && secondQuote != -1) {
            return source.substring(firstQuote + 1, secondQuote);
        }
        return "";
    }
}
