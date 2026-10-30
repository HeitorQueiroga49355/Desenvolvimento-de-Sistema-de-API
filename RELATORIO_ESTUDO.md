# Relatório de Estudo: API REST de Sistema de Chamados do IFRN

Este documento foi elaborado para servir como material completo de estudo e revisão dos conceitos de desenvolvimento de software corporativo abordados no projeto da disciplina de **Desenvolvimento de Sistemas Corporativos**.

---

## 1. Visão Geral e Arquitetura do Sistema

A API segue os princípios do padrão arquitetural em camadas (*Layered Architecture*), promovendo o baixo acoplamento (*low coupling*) e alta coesão (*high cohesion*).

```
                      ┌──────────────────────┐
                      │    Cliente HTTP      │
                      │  (Postman / Curl)    │
                      └──────────┬───────────┘
                                 │ HTTP Request (JSON)
                                 ▼
                      ┌──────────────────────┐
                      │  Controller Layer    │  @RestController
                      │  (SetorController,   │  Validação de entrada (@Valid)
                      │   ChamadoController) │  Retorno de ResponseEntity e HTTP Status
                      └──────────┬───────────┘
                                 │ DTOs / Commands
                                 ▼
                      ┌──────────────────────┐
                      │    Service Layer     │  @Service, @Transactional
                      │  (ChamadoService,    │  Todas as 8 Regras de Negócio
                      │   SetorService)      │  Lançamento de exceções de negócio
                      └──────────┬───────────┘
                                 │ Entidades de Domínio
                                 ▼
                      ┌──────────────────────┐
                      │   Repository Layer   │  JpaRepository
                      │  (ChamadoRepository) │  Consultas JPQL e filtros dinâmicos
                      └──────────┬───────────┘
                                 │ SQL nativo via Hibernate
                                 ▼
                      ┌──────────────────────┐
                      │ Banco de Dados (PG)  │  PostgreSQL 16
                      │ (Migrations Flyway)  │  Versionamento com V1 e V2
                      └──────────────────────┘
```

### 1.1 Responsabilidades de Cada Camada

1. **Controller (`br.edu.ifrn.chamados.controller`)**:
   - Ponto de entrada das requisições HTTP;
   - Recebe parâmetros, query strings e payloads JSON;
   - Executa a validação sintática via anotações Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, etc.);
   - **Não contém nenhuma regra de negócio**;
   - Delega a execução para a camada de serviço correspondente e converte o resultado em códigos de status HTTP semânticos (`200 OK`, `201 Created`, `204 No Content`).

2. **Service (`br.edu.ifrn.chamados.service`)**:
   - Núcleo de domínio e regras de negócio da aplicação;
   - Gerencia transações com `@Transactional` (garantindo atomicidade em operações de escrita e melhor desempenho com `readOnly = true` em consultas);
   - Aplica as 8 regras especificadas no documento de requisitos;
   - Lança exceções personalizadas de negócio (`RegraNegocioException`) ou de recurso (`RecursoNaoEncontradoException`).

3. **Repository (`br.edu.ifrn.chamados.repository`)**:
   - Camada de abstração de dados que estende `JpaRepository`;
   - Implementa consultas derivadas (*query methods*) e consultas declarativas com `@Query` em JPQL (*Java Persistence Query Language*);
   - Suporta paginação nativa através da interface `Pageable`.

4. **Model / Entidades (`br.edu.ifrn.chamados.model`)**:
   - Mapeamento objeto-relacional (ORM) direto para as tabelas do PostgreSQL;
   - Configurado com JPA puro sem dependência de regras de negócio internas.

5. **DTOs (`br.edu.ifrn.chamados.dto`)**:
   - Implementados utilizando **Java Records** (recurso nativo do Java moderno);
   - Imutabilidade garantida por padrão;
   - Separação estrita entre o contrato público da API (`request` e `response`) e a persistência interna do banco de dados, evitando exposição de dados sensíveis ou loops de serialização JSON.

6. **Tratamento de Exceções (`br.edu.ifrn.chamados.exception`)**:
   - `@RestControllerAdvice` centralizado no `GlobalExceptionHandler`;
   - Intercepta exceções lançadas em qualquer ponto do ciclo de requisição e retorna um JSON consistente e padronizado:
     ```json
     {
       "timestamp": "2026-10-28T22:00:00",
       "status": 409,
       "mensagem": "O técnico deve pertencer ao mesmo setor da categoria do chamado.",
       "caminho": "/chamados/1/assumir"
     }
     ```

---

## 2. Modelagem de Dados e Relacionamentos JPA

O modelo de dados implementa cinco entidades principais:

### 2.1 Entidades e Associações

| Entidade | Descrição | Relacionamentos |
| :--- | :--- | :--- |
| `Setor` | Setores da instituição (ex.: COTIC, COAS) | `1:N` com `Categoria` e `1:N` com `Usuario` (técnicos) |
| `Categoria` | Especialidades de atendimento de um setor | `N:1` com `Setor` (obrigatório) |
| `Usuario` | Solicitantes e Técnicos | `N:1` com `Setor` (obrigatório para técnicos, opcional para solicitantes) |
| `Chamado` | Registro do pedido de suporte | `N:1` com `Usuario` (solicitante), `N:1` com `Categoria`, `N:1` opcional com `Usuario` (técnico), `1:N` com `Interacao` |
| `Interacao` | Histórico e troca de mensagens no chamado | `N:1` com `Chamado` e `N:1` com `Usuario` (autor) |

### 2.2 Boas Práticas JPA Adotadas

- **Estratégia de Busca (`FetchType.LAZY`)**: Todos os relacionamentos `@ManyToOne` utilizam busca preguiçosa para evitar consultas desnecessárias (*problema do N+1*);
- **Mapeamento de Enums**: Todos os enums (`TipoUsuario`, `PrioridadeChamado`, `StatusChamado`) utilizam `@Enumerated(EnumType.STRING)` para armazenar o nome textual no banco, prevenindo que alterações na ordem das constantes corrompam os dados existentes;
- **Auditoria de Fechamento**: A data de fechamento só é atribuída pelo sistema no momento exato da resolução;
- **Validação com Hibernate**: Configurado `spring.jpa.hibernate.ddl-auto=validate`, impedindo que o Hibernate crie ou altere tabelas por conta própria e assegurando que as migrations do Flyway sejam a única fonte da verdade.

---

## 3. Ciclo de Vida do Chamado e Regras de Negócio

O ciclo de vida do chamado obedece a uma máquina de estados finita estrita:

```
                  ┌───────────────┐
                  │    ABERTO     │
                  └───┬───────┬───┘
                      │       │
      Técnico assume  │       │ Solicitante cancela
         (Regra 3)    │       │ (Regra 5)
                      ▼       ▼
      ┌──────────────────┐  ┌───────────────┐
      │  EM_ATENDIMENTO  │  │   CANCELADO   │
      └─────────┬────────┘  └───────────────┘
                │
 Técnico resolve│ (Regra 4)
                ▼
      ┌──────────────────┐
      │    RESOLVIDO     │
      └──────────────────┘
```

### 3.1 Detalhamento das 8 Regras de Negócio na Camada Service

1. **Regra 1 (Inicialização)**:
   - Todo chamado é instanciado com status `ABERTO`;
   - O campo `dataAbertura` recebe `LocalDateTime.now()` no backend;
   - Se a `prioridade` for nula na requisição, o sistema atribui automaticamente `MEDIA`.

2. **Regra 2 (Permissão de Abertura)**:
   - O solicitante informado no payload deve ter `tipo == TipoUsuario.SOLICITANTE`. Caso um usuário técnico tente abrir um chamado, o sistema lança `RegraNegocioException` (HTTP 409).

3. **Regra 3 (Atribuição do Técnico)**:
   - Um chamado só pode ser assumido se estiver com status `ABERTO`;
   - O usuário que assume deve ter `tipo == TipoUsuario.TECNICO`;
   - O setor do técnico (`tecnico.getSetor()`) deve coincidir exatamente com o setor da categoria do chamado (`chamado.getCategoria().getSetor()`). Caso contrário, lança HTTP 409;
   - Ao assumir com sucesso, o status passa para `EM_ATENDIMENTO` e o técnico é gravado na entidade.

4. **Regra 4 (Resolução do Chamado)**:
   - Somente chamados no estado `EM_ATENDIMENTO` podem ser resolvidos;
   - Somente o técnico previamente atribuído (`chamado.getTecnico()`) pode executar a resolução;
   - A descrição da solução é obrigatória e não pode conter apenas espaços em branco;
   - Ao resolver, `dataFechamento` é gravada com a data e hora do servidor e o status muda para `RESOLVIDO`.

5. **Regra 5 (Cancelamento do Chamado)**:
   - Somente o próprio solicitante que abriu o chamado (`chamado.getSolicitante()`) pode cancelá-lo;
   - O cancelamento só é permitido se o status ainda for `ABERTO` (não é permitido cancelar após ter entrado em atendimento);
   - O status transiciona para `CANCELADO`.

6. **Regra 6 (Imutabilidade de Estados Terminais)**:
   - Chamados nos status terminais (`RESOLVIDO` ou `CANCELADO`) são imutáveis;
   - Não aceitam alterações de status nem inserção de novas mensagens de interação.

7. **Regra 7 (Permissão para Registrar Interações)**:
   - Apenas o solicitante autor do chamado ou o técnico atribuído têm autorização para registrar mensagens na linha do tempo do chamado;
   - Qualquer outro usuário que tente interagir recebe HTTP 409.

8. **Regra 8 (Proteção de Integridade Referencial)**:
   - Antes de excluir um `Setor` ou uma `Categoria`, o sistema consulta se existem chamados vinculados no banco de dados;
   - Se houver qualquer chamado associado, a exclusão é abortada com HTTP 409.

---

## 4. Paginação, Ordenação e Filtros com Spring Data

Todas as rotas de listagem (retorno de coleção) implementam paginação com `Pageable`:

- Parâmetros aceitos na query string:
  - `page`: índice da página (iniciando em 0);
  - `size`: quantidade de registros por página (padrão de 10, configurado no `application.properties` com teto máximo de 50);
  - `sort`: campo e direção da ordenação (exemplo: `sort=dataAbertura,desc`).
- A resposta envelopada na classe `PageResponse<T>` retorna:
  - `itens`: lista dos objetos convertidos para DTO de resposta;
  - `numeroPagina`: número da página atual;
  - `tamanho`: tamanho solicitado;
  - `totalElementos`: contagem total de registros no banco que atendem aos filtros;
  - `totalPaginas`: quantidade total de páginas disponíveis.

### 4.1 Filtros Combináveis em JPQL

Os filtros foram modelados com queries que tratam parâmetros nulos de forma transparente:
```sql
SELECT c FROM Chamado c WHERE
(:status IS NULL OR c.status = :status) AND
(:prioridade IS NULL OR c.prioridade = :prioridade) AND
(:setorId IS NULL OR c.categoria.setor.id = :setorId) AND
(:categoriaId IS NULL OR c.categoria.id = :categoriaId) AND
(:solicitanteId IS NULL OR c.solicitante.id = :solicitanteId) AND
(:tecnicoId IS NULL OR (c.tecnico IS NOT NULL AND c.tecnico.id = :tecnicoId)) AND
(:dataInicio IS NULL OR c.dataAbertura >= :dataInicio) AND
(:dataFim IS NULL OR c.dataAbertura <= :dataFim)
```
Se o cliente não enviar determinado filtro, o valor é interpretado como `null` e a respectiva cláusula é ignorada na filtragem.

---

## 5. Versionamento de Banco de Dados com Flyway

O Flyway gerencia as migrações do banco de dados na pasta `src/main/resources/db/migration/`:

- `V1__criar_tabelas.sql`: Define a criação do esquema relacional completo (tabelas `setor`, `categoria`, `usuario`, `chamado`, `interacao`, chaves primárias e estrangeiras);
- `V2__inserir_dados_iniciais.sql`: Insere os dados sementes obrigatórios (2 setores: COTIC e COAS; 4 categorias; 4 usuários: 2 solicitantes e 2 técnicos).

### 5.1 Mecanismo de Checksum e Auditoria
O Flyway calcula uma soma de verificação CRC32 para cada arquivo. Quando um arquivo já aplicado no banco sofre qualquer alteração, o checksum local não bate com o da tabela `flyway_schema_history`, impedindo a subida da aplicação para proteger contra alterações que deixariam o banco em estado inconsistente.

---

## 6. Conteinerização com Docker e Docker Compose

O projeto implementa uma abordagem de entrega contínua padrão da indústria:

### 6.1 Dockerfile Multi-Stage
1. **Estágio 1 (`build`)**: Usa a imagem oficial `maven:3.9.9-eclipse-temurin-21` para compilar o código-fonte e empacotar o arquivo JAR (`mvn clean package -DskipTests`);
2. **Estágio 2 (`runtime`)**: Descarta todo o código-fonte, o Maven e os arquivos temporários, copiando apenas o arquivo `.jar` gerado para uma imagem limpa `eclipse-temurin:21-jre-alpine`. Isso reduz o tamanho final da imagem e elimina vulnerabilidades de segurança desnecessárias.

### 6.2 Orquestração no `docker-compose.yml`
- Serviço `postgres`: Container com PostgreSQL 16 Alpine, volume nomeado `postgres_data` para persistência dos dados entre reinicializações, e um `healthcheck` que testa `pg_isready`;
- Serviço `api`: Constrói a aplicação e aguarda através de `depends_on` que o container do Postgres esteja saudável (`service_healthy`) antes de iniciar o Spring Boot. Assim, elimina-se qualquer erro de conexão na inicialização.

---

## 7. Roteiro Prático de Estudo (Testando o Fluxo Completo)

Para fixar todo o ciclo de vida e demonstrar o funcionamento prático:

1. Suba o ambiente:
   ```bash
   docker compose up --build
   ```
2. Abra um chamado como Solicitante (ID 1) na categoria de Redes (ID 1):
   - `POST /chamados` -> Resposta `201 Created`, status `ABERTO`.
3. Tente assumir com um técnico de Manutenção (Setor 2):
   - `PATCH /chamados/1/assumir` com `tecnicoId: 4` -> Resposta `409 Conflict` (violação da Regra 3).
4. Assuma com o técnico correto da COTIC (Setor 1, ID 3):
   - `PATCH /chamados/1/assumir` com `tecnicoId: 3` -> Resposta `200 OK`, status muda para `EM_ATENDIMENTO`.
5. Registre uma interação entre técnico e solicitante:
   - `POST /chamados/1/interacoes` -> Resposta `201 Created`.
6. Resolva o chamado com o técnico atribuído:
   - `PATCH /chamados/1/resolver` com `tecnicoId: 3` e solução preenchida -> Resposta `200 OK`, status `RESOLVIDO` e `dataFechamento` preenchida.
7. Tente adicionar uma interação após a resolução:
   - `POST /chamados/1/interacoes` -> Resposta `409 Conflict` (violação da Regra 6).
