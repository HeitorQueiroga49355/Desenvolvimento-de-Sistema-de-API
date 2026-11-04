package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCategoriaRepository implements CategoriaRepository {

    private static final String SELECT = """
            SELECT c.id, c.nome, c.descricao,
                   s.id AS setor_id, s.nome AS setor_nome, s.sigla AS setor_sigla
            FROM categoria c
            JOIN setor s ON s.id = c.setor_id
            """;

    private static final String FILTROS = """
            WHERE (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
              AND (:setorId IS NULL OR c.setor_id = :setorId)
            """;

    static final RowMapper<Categoria> ROW_MAPPER = (rs, rowNum) -> new Categoria(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("descricao"),
            new Setor(rs.getLong("setor_id"), rs.getString("setor_nome"), rs.getString("setor_sigla"))
    );

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCategoriaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Categoria> findById(Long id) {
        return jdbc.query(SELECT + " WHERE c.id = :id", new MapSqlParameterSource("id", id), ROW_MAPPER)
                .stream().findFirst();
    }

    public List<Categoria> findBySetorId(Long setorId) {
        return jdbc.query(SELECT + " WHERE c.setor_id = :setorId ORDER BY c.id",
                new MapSqlParameterSource("setorId", setorId), ROW_MAPPER);
    }

    public Page<Categoria> findBySetorId(Long setorId, Pageable pageable) {
        return buscarComFiltros(null, setorId, pageable);
    }

    public Page<Categoria> buscarComFiltros(String nome, Long setorId, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nome", nome)
                .addValue("setorId", setorId);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM categoria c " + FILTROS, params, Long.class);

        params.addValue("limit", pageable.getPageSize())
              .addValue("offset", pageable.getOffset());
        List<Categoria> conteudo = jdbc.query(
                SELECT + FILTROS + " ORDER BY c.id LIMIT :limit OFFSET :offset", params, ROW_MAPPER);

        return new PageImpl<>(conteudo, pageable, total == null ? 0 : total);
    }

    @Override
    public Categoria save(Categoria categoria) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nome", categoria.getNome())
                .addValue("descricao", categoria.getDescricao())
                .addValue("setorId", categoria.getSetor().getId());

        if (categoria.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO categoria (nome, descricao, setor_id) VALUES (:nome, :descricao, :setorId)",
                    params, keys, new String[]{"id"});
            categoria.setId(keys.getKey().longValue());
        } else {
            params.addValue("id", categoria.getId());
            jdbc.update("UPDATE categoria SET nome = :nome, descricao = :descricao, setor_id = :setorId WHERE id = :id",
                    params);
        }
        return categoria;
    }

    @Override
    public void delete(Categoria categoria) {
        jdbc.update("DELETE FROM categoria WHERE id = :id", new MapSqlParameterSource("id", categoria.getId()));
    }
}
