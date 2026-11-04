package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcChamadoRepository implements ChamadoRepository {

    private static final String SELECT = """
            SELECT c.id, c.titulo, c.descricao, c.local, c.prioridade, c.status,
                   c.data_abertura, c.data_fechamento, c.solucao,
                   sol.id AS sol_id, sol.nome AS sol_nome, sol.email AS sol_email,
                   sol.matricula AS sol_matricula, sol.tipo AS sol_tipo,
                   cat.id AS cat_id, cat.nome AS cat_nome, cat.descricao AS cat_descricao,
                   stt.id AS set_id, stt.nome AS set_nome, stt.sigla AS set_sigla,
                   tec.id AS tec_id, tec.nome AS tec_nome, tec.email AS tec_email,
                   tec.matricula AS tec_matricula, tec.tipo AS tec_tipo
            FROM chamado c
            JOIN usuario sol ON sol.id = c.solicitante_id
            JOIN categoria cat ON cat.id = c.categoria_id
            JOIN setor stt ON stt.id = cat.setor_id
            LEFT JOIN usuario tec ON tec.id = c.tecnico_id
            """;

    private static final String FROM_FILTROS = """
            FROM chamado c
            JOIN categoria cat ON cat.id = c.categoria_id
            WHERE (CAST(:status AS VARCHAR) IS NULL OR c.status = :status)
              AND (CAST(:prioridade AS VARCHAR) IS NULL OR c.prioridade = :prioridade)
              AND (CAST(:setorId AS BIGINT) IS NULL OR cat.setor_id = :setorId)
              AND (CAST(:categoriaId AS BIGINT) IS NULL OR c.categoria_id = :categoriaId)
              AND (CAST(:solicitanteId AS BIGINT) IS NULL OR c.solicitante_id = :solicitanteId)
              AND (CAST(:tecnicoId AS BIGINT) IS NULL OR c.tecnico_id = :tecnicoId)
              AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR c.data_abertura >= :dataInicio)
              AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR c.data_abertura <= :dataFim)
            """;

    private static final String FROM_USUARIO = """
            FROM chamado c
            WHERE (c.solicitante_id = :usuarioId OR c.tecnico_id = :usuarioId)
              AND (CAST(:status AS VARCHAR) IS NULL OR c.status = :status)
            """;

    /** Propriedades de ordenação aceitas (evita SQL injection via parâmetro sort). */
    private static final Map<String, String> COLUNAS_ORDENAVEIS = Map.of(
            "id", "c.id",
            "titulo", "c.titulo",
            "prioridade", "c.prioridade",
            "status", "c.status",
            "dataAbertura", "c.data_abertura",
            "dataFechamento", "c.data_fechamento"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcChamadoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- Consultas simples ----------

    public Optional<Chamado> findById(Long id) {
        Optional<Chamado> chamado = jdbc.query(
                SELECT + " WHERE c.id = :id", new MapSqlParameterSource("id", id), CHAMADO_MAPPER)
                .stream().findFirst();
        chamado.ifPresent(c -> c.setInteracoes(buscarInteracoes(c)));
        return chamado;
    }

    @Override
    public boolean existsByCategoriaId(Long categoriaId) {
        return jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM chamado WHERE categoria_id = :id)",
                new MapSqlParameterSource("id", categoriaId), Boolean.class);
    }

    @Override
    public boolean existsByCategoriaSetorId(Long setorId) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM chamado c
                               JOIN categoria cat ON cat.id = c.categoria_id
                               WHERE cat.setor_id = :id)
                """, new MapSqlParameterSource("id", setorId), Boolean.class);
    }

    // ---------- Consultas com filtros e paginação ----------

    public Page<Chamado> buscarComFiltros(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", status == null ? null : status.name(), Types.VARCHAR)
                .addValue("prioridade", prioridade == null ? null : prioridade.name(), Types.VARCHAR)
                .addValue("setorId", setorId, Types.BIGINT)
                .addValue("categoriaId", categoriaId, Types.BIGINT)
                .addValue("solicitanteId", solicitanteId, Types.BIGINT)
                .addValue("tecnicoId", tecnicoId, Types.BIGINT)
                .addValue("dataInicio", dataInicio == null ? null : Timestamp.valueOf(dataInicio), Types.TIMESTAMP)
                .addValue("dataFim", dataFim == null ? null : Timestamp.valueOf(dataFim), Types.TIMESTAMP);

        return paginar(FROM_FILTROS, params, pageable);
    }

    public Page<Chamado> buscarPorUsuarioEStatus(Long usuarioId, StatusChamado status, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("usuarioId", usuarioId, Types.BIGINT)
                .addValue("status", status == null ? null : status.name(), Types.VARCHAR);

        return paginar(FROM_USUARIO, params, pageable);
    }

    private Page<Chamado> paginar(String fromWhere, MapSqlParameterSource params, Pageable pageable) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + fromWhere, params, Long.class);

        // Primeiro pega só os ids da página; depois carrega os chamados completos.
        params.addValue("limit", pageable.getPageSize())
              .addValue("offset", pageable.getOffset());
        List<Long> ids = jdbc.queryForList(
                "SELECT c.id " + fromWhere + " ORDER BY " + ordenacao(pageable.getSort())
                        + " LIMIT :limit OFFSET :offset",
                params, Long.class);

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total == null ? 0 : total);
        }

        Map<Long, Chamado> porId = jdbc.query(
                SELECT + " WHERE c.id IN (:ids)", new MapSqlParameterSource("ids", ids), CHAMADO_MAPPER)
                .stream().collect(Collectors.toMap(Chamado::getId, c -> c));

        // Mantém a ordem definida pela consulta de ids.
        List<Chamado> conteudo = ids.stream().map(porId::get).toList();
        return new PageImpl<>(conteudo, pageable, total == null ? 0 : total);
    }

    private String ordenacao(Sort sort) {
        String ordem = sort.stream()
                .filter(o -> COLUNAS_ORDENAVEIS.containsKey(o.getProperty()))
                .map(o -> COLUNAS_ORDENAVEIS.get(o.getProperty()) + (o.isAscending() ? " ASC" : " DESC"))
                .collect(Collectors.joining(", "));
        return ordem.isEmpty() ? "c.id" : ordem + ", c.id";
    }

    // ---------- Escrita ----------

    /**
     * Insere ou atualiza o chamado. As interações NÃO são gravadas aqui:
     * elas são persistidas pelo InteracaoRepository.
     */
    @Override
    public Chamado save(Chamado chamado) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("titulo", chamado.getTitulo())
                .addValue("descricao", chamado.getDescricao())
                .addValue("local", chamado.getLocalizacao())
                .addValue("prioridade", chamado.getPrioridade().name())
                .addValue("status", chamado.getStatus().name())
                .addValue("dataAbertura", Timestamp.valueOf(chamado.getDataAbertura()))
                .addValue("dataFechamento",
                        chamado.getDataFechamento() == null ? null : Timestamp.valueOf(chamado.getDataFechamento()),
                        Types.TIMESTAMP)
                .addValue("solucao", chamado.getSolucao(), Types.VARCHAR)
                .addValue("solicitanteId", chamado.getSolicitante().getId())
                .addValue("categoriaId", chamado.getCategoria().getId())
                .addValue("tecnicoId",
                        chamado.getTecnico() == null ? null : chamado.getTecnico().getId(),
                        Types.BIGINT);

        if (chamado.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update("""
                    INSERT INTO chamado (titulo, descricao, local, prioridade, status, data_abertura,
                                         data_fechamento, solucao, solicitante_id, categoria_id, tecnico_id)
                    VALUES (:titulo, :descricao, :local, :prioridade, :status, :dataAbertura,
                            :dataFechamento, :solucao, :solicitanteId, :categoriaId, :tecnicoId)
                    """, params, keys, new String[]{"id"});
            chamado.setId(keys.getKey().longValue());
        } else {
            params.addValue("id", chamado.getId());
            jdbc.update("""
                    UPDATE chamado
                       SET titulo = :titulo, descricao = :descricao, local = :local,
                           prioridade = :prioridade, status = :status, data_abertura = :dataAbertura,
                           data_fechamento = :dataFechamento, solucao = :solucao,
                           solicitante_id = :solicitanteId, categoria_id = :categoriaId,
                           tecnico_id = :tecnicoId
                     WHERE id = :id
                    """, params);
        }
        return chamado;
    }

    // ---------- Mapeamento ----------

    private List<Interacao> buscarInteracoes(Chamado chamado) {
        return jdbc.query("""
                SELECT i.id, i.texto, i.data_hora,
                       u.id AS aut_id, u.nome AS aut_nome, u.email AS aut_email,
                       u.matricula AS aut_matricula, u.tipo AS aut_tipo
                FROM interacao i
                JOIN usuario u ON u.id = i.autor_id
                WHERE i.chamado_id = :id
                ORDER BY i.data_hora ASC, i.id ASC
                """, new MapSqlParameterSource("id", chamado.getId()),
                (rs, rowNum) -> new Interacao(
                        rs.getLong("id"),
                        rs.getString("texto"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        chamado,
                        usuario(rs, "aut_")));
    }

    private static Usuario usuario(ResultSet rs, String prefixo) throws SQLException {
        long id = rs.getLong(prefixo + "id");
        if (rs.wasNull()) {
            return null;
        }
        return new Usuario(
                id,
                rs.getString(prefixo + "nome"),
                rs.getString(prefixo + "email"),
                rs.getString(prefixo + "matricula"),
                TipoUsuario.valueOf(rs.getString(prefixo + "tipo")),
                null);
    }

    private static final RowMapper<Chamado> CHAMADO_MAPPER = (rs, rowNum) -> {
        Timestamp fechamento = rs.getTimestamp("data_fechamento");
        Categoria categoria = new Categoria(
                rs.getLong("cat_id"),
                rs.getString("cat_nome"),
                rs.getString("cat_descricao"),
                new Setor(rs.getLong("set_id"), rs.getString("set_nome"), rs.getString("set_sigla")));

        return new Chamado(
                rs.getLong("id"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getString("local"),
                PrioridadeChamado.valueOf(rs.getString("prioridade")),
                StatusChamado.valueOf(rs.getString("status")),
                rs.getTimestamp("data_abertura").toLocalDateTime(),
                fechamento == null ? null : fechamento.toLocalDateTime(),
                rs.getString("solucao"),
                usuario(rs, "sol_"),
                categoria,
                usuario(rs, "tec_"));
    };
}
