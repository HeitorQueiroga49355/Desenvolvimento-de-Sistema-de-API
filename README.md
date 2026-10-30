# API REST — Sistema de Chamados do IFRN

API REST desenvolvida em Spring Boot e PostgreSQL para gerenciamento centralizado de chamados de suporte do IFRN (Campus Pau dos Ferros). O sistema adota arquitetura em camadas, persistência com Spring Data JPA, versionamento de banco com Flyway e conteinerização completa com Docker e Docker Compose.

---

## 🚀 Como Executar com Docker

Todo o ambiente (banco de dados PostgreSQL e API) é executado via Docker Compose, sem necessidade de configuração prévia na máquina além do Docker.

### 1. Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose instalados.

### 2. Inicialização
Na raiz do projeto, execute:

```bash
docker compose up --build
```

A API estará disponível e pronta para uso em:
```
http://localhost:8080
```

Para encerrar a execução e preservar os dados:
```bash
docker compose down
```

---

## 🗄️ Estudo sobre Migrations e Flyway

Conforme requisito de estudo da disciplina, segue a explicação técnica sobre versionamento de esquema com Flyway e o motivo do descarte do `ddl-auto=update`:

### 1. Como o Flyway registra versões na tabela `flyway_schema_history`
Ao iniciar a aplicação com Flyway habilitado, o Flyway cria e mantém uma tabela de controle chamada `flyway_schema_history`. Cada arquivo de migration (`V1__...`, `V2__...`) executado com sucesso recebe um registro contendo:
- `installed_rank`: ordem cronológica de aplicação;
- `version`: número da versão extraído do nome do arquivo;
- `description`: descrição textual da migration;
- `type`: tipo da migração (normalmente `SQL`);
- `script`: nome do arquivo SQL executado;
- `checksum`: hash criptográfico (CRC32) gerado a partir do conteúdo do arquivo no momento em que ele foi aplicado;
- `installed_by`: usuário do banco que realizou a aplicação;
- `installed_on`: data e hora da execução;
- `execution_time`: tempo de execução em milissegundos;
- `success`: booleano indicando se a execução foi concluída sem falhas.

Nas inicializações seguintes, o Flyway compara os arquivos locais com essa tabela para determinar se há scripts pendentes ou se a integridade do histórico foi violada.

### 2. O que acontece quando o checksum de uma migration aplicada muda
Se um arquivo de migration já aplicado (por exemplo, `V1__criar_tabelas.sql`) for editado (mesmo um espaço ou nova linha), seu hash CRC32 será recalculado pelo Flyway na inicialização e não baterá com o `checksum` registrado em `flyway_schema_history`.

O Flyway detecta a divergência e **aborta a inicialização da aplicação** com uma exceção (`FlywayValidateException`). Esse mecanismo protege o banco de dados contra alterações retroativas não rastreadas, garantindo idempotência e reprodutibilidade entre ambientes. Por esse motivo, qualquer ajuste, correção ou novo campo exige a criação de uma nova migration sequencial (ex.: `V3__...`).

### 3. Por que `spring.jpa.hibernate.ddl-auto=update` não é usado em produção
O modo `update` do Hibernate tenta deduzir e aplicar alterações no banco de dados automaticamente com base nas anotações das entidades JPA. Na prática corporativa, seu uso em produção é estritamente evitado pelos seguintes motivos:
- **Risco de inconsistência e indisponibilidade**: o Hibernate não executa validação prévia de transações complexas, podendo travar tabelas inteiras em momentos de pico;
- **Não remove colunas nem tabelas obsoletas**: ele apenas adiciona colunas ou tabelas, deixando lixo estrutural e esquemas órfãos;
- **Falta de controle e auditoria**: não há histórico de quem alterou o que e quando, tornando impossível reverter (*rollback*) migrações problemáticas;
- **Divergência entre ambientes**: desenvolvedores podem ter esquemas locais divergentes do ambiente de homologação ou produção sem aviso prévio.

Em ambientes de produção, adota-se ferramentas de migração explícitas como Flyway com `spring.jpa.hibernate.ddl-auto=validate`, garantindo que o código apenas valide se a estrutura do banco coincide rigorosamente com as entidades.

---

## 📌 Exemplos de Requisição

Todas as requisições estão documentadas e prontas para execução nos formatos:
- Arquivo HTTP padrão: [`requisicoes.http`](./requisicoes.http);
- Coleção para importação direta no Insomnia: [`insomnia.json`](./insomnia.json).

### Abrir Chamado
- **Método**: `POST`
- **Rota**: `/chamados`
- **Body**:
```json
{
  "titulo": "Wi-Fi oscilando no Bloco B",
  "descricao": "Conexão sem fio caindo intermitentemente",
  "local": "Bloco B - Sala 14",
  "prioridade": "ALTA",
  "solicitanteId": 1,
  "categoriaId": 1
}
```
- **Resposta (201 Created)**:
```json
{
  "id": 1,
  "titulo": "Wi-Fi oscilando no Bloco B",
  "descricao": "Conexão sem fio caindo intermitentemente",
  "local": "Bloco B - Sala 14",
  "prioridade": "ALTA",
  "status": "ABERTO",
  "dataAbertura": "2026-10-28T22:00:00",
  "dataFechamento": null,
  "solucao": null,
  "solicitanteId": 1,
  "solicitanteNome": "Alice Silva",
  "categoriaId": 1,
  "categoriaNome": "Rede e Wi-Fi",
  "setorId": 1,
  "setorSigla": "COTIC",
  "tecnicoId": null,
  "tecnicoNome": null
}
```

### Técnico Assume Chamado
- **Método**: `PATCH`
- **Rota**: `/chamados/1/assumir`
- **Body**:
```json
{
  "tecnicoId": 3
}
```

### Adicionar Interação
- **Método**: `POST`
- **Rota**: `/chamados/1/interacoes`
- **Body**:
```json
{
  "autorId": 3,
  "texto": "Equipamento reiniciado. Aguardando teste do solicitante."
}
```

### Resolver Chamado
- **Método**: `PATCH`
- **Rota**: `/chamados/1/resolver`
- **Body**:
```json
{
  "tecnicoId": 3,
  "solucao": "Access point substituído e canal de frequência reconfigurado."
}
```

### Cancelar Chamado
- **Método**: `PATCH`
- **Rota**: `/chamados/1/cancelar`
- **Body**:
```json
{
  "solicitanteId": 1
}
```

---

## 🧪 Execução de Testes Automatizados

O projeto inclui suíte de testes unitários cobrindo as 8 regras de negócio do sistema:

```bash
mvn test
```
