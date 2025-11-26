package com.rinha.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class DatabaseSetup {

    @Inject
    DataSource dataSource;

    @PostConstruct
    public void init() {

        String sqlProcessTransaction = """
            CREATE OR REPLACE FUNCTION process_transaction(
                p_account_id INT,
                p_amount INT,
                p_type CHAR,
                p_description VARCHAR(10)
            )
            RETURNS JSON AS $$
            DECLARE
                response JSON;
            BEGIN
                WITH updated_account AS (
                    UPDATE accounts
                    SET balance = balance + CASE WHEN p_type = 'c' THEN p_amount ELSE -p_amount END
                    WHERE id = p_account_id 
                      AND (p_type = 'c' OR (balance - p_amount) >= -account_limit)
                    RETURNING balance, account_limit
                ),
                inserted_transaction AS (
                    INSERT INTO transactions (account_id, amount, type, description)
                    SELECT p_account_id, p_amount, p_type, p_description
                    FROM updated_account
                    RETURNING 1
                )
                SELECT json_build_object('saldo', ua.balance, 'limite', ua.account_limit)
                INTO response
                FROM updated_account ua;

                IF response IS NULL THEN
                    RETURN '{"error": 1}'::json;
                END IF;

                RETURN response;
            END;
            $$ LANGUAGE plpgsql;
        """;

        String sqlGetExtrato = """
            CREATE OR REPLACE FUNCTION get_extrato(p_account_id INT)
            RETURNS JSON AS $$
            DECLARE
                account_info JSON;
                last_transactions JSON;
            BEGIN
                SELECT json_build_object(
                    'total', balance,
                    'limite', account_limit,
                    'data_extrato', CURRENT_TIMESTAMP
                )
                INTO account_info
                FROM accounts
                WHERE id = p_account_id;

                IF account_info IS NULL THEN
                    RETURN NULL;
                END IF;

                SELECT json_agg(t)
                INTO last_transactions
                FROM (
                    SELECT amount AS valor, type AS tipo, description AS descricao, created_at AS realizada_em
                    FROM transactions
                    WHERE account_id = p_account_id
                    ORDER BY id DESC
                    LIMIT 10
                ) t;

                RETURN json_build_object(
                    'saldo', account_info,
                    'ultimas_transacoes', COALESCE(last_transactions, '[]'::json)
                );
            END;
            $$ LANGUAGE plpgsql;
        """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.execute(sqlProcessTransaction);
            System.out.println("Function 'process_transaction' criada/atualizada.");

            statement.execute(sqlGetExtrato);
            System.out.println("Function 'get_extrato' criada/atualizada.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro fatal ao configurar functions no banco de dados", e);
        }
    }
}