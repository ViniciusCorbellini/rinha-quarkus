CREATE TABLE IF NOT EXISTS accounts (
    id INT PRIMARY KEY,
    limite INT NOT NULL,
    balance INT NOT NULL,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id INT NOT NULL REFERENCES accounts(id),
    amount INT NOT NULL,
    type CHAR(1) NOT NULL CHECK (type IN ('c', 'd')),
    description VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transactions_account_id_id
  ON transactions(account_id, id DESC);

DO $$
BEGIN
  IF NOT EXISTS (SELECT * FROM accounts WHERE id BETWEEN 1 AND 5) THEN
    INSERT INTO accounts (id, limite, balance)
    VALUES
    (1, 100000, 0),
    (2, 80000, 0),
    (3, 1000000, 0),
    (4, 10000000, 0),
    (5, 500000, 0);
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION obter_extrato(p_account_id INT)
RETURNS TABLE (
    total NUMERIC,
    limite NUMERIC,
    data_extrato TIMESTAMP WITH TIME ZONE,
    trans_valor NUMERIC,
    trans_tipo CHAR(1),
    trans_descricao VARCHAR(10),
    trans_realizada_em TIMESTAMP 
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        a.balance::NUMERIC,
        a.limite::NUMERIC,
        now(),
        t.amount::NUMERIC,
        t.type,
        t.description,
        t.created_at 
    FROM
        accounts a
    LEFT JOIN (
        SELECT account_id, amount, type, description, created_at
        FROM transactions
        WHERE account_id = p_account_id
        ORDER BY created_at DESC
        LIMIT 10
    ) t ON a.id = t.account_id
    WHERE
        a.id = p_account_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION realizar_transacao(
    p_account_id INT,
    p_valor NUMERIC,
    p_tipo CHAR(1),
    p_descricao VARCHAR(10)
)
RETURNS TABLE (novo_saldo INT, novo_limite INT) AS $$
DECLARE
    conta RECORD;
    v_novo_saldo INT;
BEGIN
    -- 1. Trava a linha e busca os dados
    SELECT balance, limite INTO conta FROM accounts WHERE id = p_account_id FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'CLIENTE_NAO_ENCONTRADO'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Lógica de negócio
    IF p_tipo = 'c' THEN
        v_novo_saldo := conta.balance + p_valor;
    ELSE
        v_novo_saldo := conta.balance - p_valor;
        IF v_novo_saldo < (conta.limite * -1) THEN
            RAISE EXCEPTION 'LIMITE_INDISPONIVEL'
                USING ERRCODE = 'P0002';
        END IF;
    END IF;

    -- 3. Persiste os dados
    UPDATE accounts SET balance = v_novo_saldo WHERE id = p_account_id;
    INSERT INTO transactions (account_id, amount, type, description)
    VALUES (p_account_id, p_valor, p_tipo, p_descricao);

    -- 4. Retorna o resultado
    RETURN QUERY SELECT v_novo_saldo AS novo_saldo, conta.limite AS novo_limite;
END;
$$ LANGUAGE plpgsql;
