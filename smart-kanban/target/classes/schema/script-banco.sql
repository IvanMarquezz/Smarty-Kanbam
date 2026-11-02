-- Script de REFERÊNCIA do modelo (SQLite). Em execução normal o Hibernate cria as tabelas
-- sozinho (spring.jpa.hibernate.ddl-auto=update). Use este arquivo para estudar/mostrar o modelo:
--   sqlite3 smartkanban.db < src/main/resources/schema/script-banco.sql

CREATE TABLE IF NOT EXISTS usuario (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    nome   VARCHAR(255) NOT NULL,
    email  VARCHAR(255) NOT NULL,
    senha  VARCHAR(255) NOT NULL            -- hash SHA-256, nunca texto puro
);

CREATE TABLE IF NOT EXISTS quadro (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        VARCHAR(255) NOT NULL,
    descricao   VARCHAR(255),
    usuario_id  INTEGER NOT NULL REFERENCES usuario(id)      -- Usuario 1 --- N Quadro
);

CREATE TABLE IF NOT EXISTS tarefa (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo          VARCHAR(255) NOT NULL,
    descricao       VARCHAR(2000),
    prioridade      VARCHAR(255) NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA')),
    status          VARCHAR(255) NOT NULL CHECK (status IN ('A_FAZER','EM_ANDAMENTO','CONCLUIDA')),
    data_criacao    VARCHAR(255) NOT NULL,                    -- texto ISO-8601
    prazo           VARCHAR(255),
    data_conclusao  VARCHAR(255),
    estimativa      VARCHAR(255),                             -- sugerida pela IA
    quadro_id       INTEGER NOT NULL REFERENCES quadro(id),   -- Quadro 1 --- N Tarefa
    tarefa_pai_id   INTEGER REFERENCES tarefa(id)             -- auto-relação: subtarefas
);
