CREATE TABLE setor (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    sigla VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    setor_id BIGINT NOT NULL,
    CONSTRAINT fk_categoria_setor FOREIGN KEY (setor_id) REFERENCES setor(id)
);

CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    matricula VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    setor_id BIGINT,
    CONSTRAINT fk_usuario_setor FOREIGN KEY (setor_id) REFERENCES setor(id)
);

CREATE TABLE chamado (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    local VARCHAR(100) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_fechamento TIMESTAMP,
    solucao TEXT,
    solicitante_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    tecnico_id BIGINT,
    CONSTRAINT fk_chamado_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuario(id),
    CONSTRAINT fk_chamado_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_chamado_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuario(id)
);

CREATE TABLE interacao (
    id BIGSERIAL PRIMARY KEY,
    texto TEXT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    chamado_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    CONSTRAINT fk_interacao_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
    CONSTRAINT fk_interacao_autor FOREIGN KEY (autor_id) REFERENCES usuario(id)
);
