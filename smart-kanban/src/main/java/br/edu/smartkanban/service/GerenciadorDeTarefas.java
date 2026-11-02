package br.edu.smartkanban.service;

import br.edu.smartkanban.dto.EstadoDesfazerResponse;
import br.edu.smartkanban.dto.SubtarefaSugeridaDTO;
import br.edu.smartkanban.dto.TarefaRequest;
import br.edu.smartkanban.dto.TarefaResponse;
import br.edu.smartkanban.estrutura.*;
import br.edu.smartkanban.excecao.RecursoNaoEncontradoException;
import br.edu.smartkanban.model.*;
import br.edu.smartkanban.repository.QuadroRepository;
import br.edu.smartkanban.repository.TarefaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coração do sistema: CRUD de tarefas + controle da PILHA de desfazer.
 *
 * Fluxo de toda operação desfazível:
 *   1) guarda o estado ANTERIOR;  2) altera o banco;  3) PUSH de uma Acao na pilha.
 * Fluxo do desfazer: PEEK (olha o topo) -> acao.desfazer() (altera o banco) -> POP (remove do topo).
 *
 * @Transactional: cada método roda numa transação; se der erro, o banco sofre rollback.
 */
@Service
@Transactional
public class GerenciadorDeTarefas implements OperacoesDeDesfazer {

    private static final int LIMITE_PILHA = 50;

    private final TarefaRepository tarefas;
    private final QuadroRepository quadros;

    @PersistenceContext
    private EntityManager em;

    /** COMPOSIÇÃO: o gerenciador POSSUI a pilha (ela nasce e morre com ele). */
    private final PilhaDeAcoes pilha = new PilhaDeAcoes(LIMITE_PILHA);

    public GerenciadorDeTarefas(TarefaRepository tarefas, QuadroRepository quadros) {
        this.tarefas = tarefas;
        this.quadros = quadros;
    }

    // ===================== READ =====================

    @Transactional(readOnly = true)
    public List<TarefaResponse> listar() {
        return tarefas.findByQuadroIdOrderByIdAsc(quadroPadrao().getId())
                .stream().map(TarefaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TarefaResponse buscar(Long id) {
        return TarefaResponse.de(buscarEntidade(id));
    }

    // ===================== CREATE =====================

    public TarefaResponse criar(TarefaRequest r) {
        validar(r);
        Tarefa t = new Tarefa(r.titulo().trim(), limpar(r.descricao()),
                r.prioridade() == null ? Prioridade.MEDIA : r.prioridade(), r.prazo(), quadroPadrao());
        tarefas.save(t); // INSERT (o id é gerado pelo banco)
        pilha.push(new AcaoCriarTarefa("Criar \"" + t.getTitulo() + "\"", List.of(t.getId())));
        return TarefaResponse.de(t);
    }

    /** Salva as subtarefas que o usuário ACEITOU da IA, vinculadas à tarefa principal. */
    public List<TarefaResponse> criarSubtarefas(Long paiId, List<SubtarefaSugeridaDTO> aceitas) {
        Tarefa pai = buscarEntidade(paiId);
        if (aceitas == null || aceitas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma subtarefa foi selecionada.");
        }
        List<Tarefa> novas = new ArrayList<>();
        for (SubtarefaSugeridaDTO s : aceitas) {
            if (s.titulo() == null || s.titulo().isBlank()) continue;
            Tarefa sub = new Tarefa(s.titulo().trim(), limpar(s.descricao()), pai.getPrioridade(), pai.getPrazo(), pai.getQuadro());
            sub.setEstimativa(limpar(s.estimativa()));
            pai.adicionarSubtarefa(sub); // vincula tarefa_pai_id
            novas.add(sub);
        }
        tarefas.saveAndFlush(pai); // cascade grava as subtarefas e gera seus ids
        List<Long> ids = novas.stream().map(Tarefa::getId).toList();
        pilha.push(new AcaoCriarTarefa("Salvar " + ids.size() + " subtarefas de \"" + pai.getTitulo() + "\"", ids));
        return novas.stream().map(TarefaResponse::de).toList();
    }

    // ===================== UPDATE =====================

    public TarefaResponse editar(Long id, TarefaRequest r) {
        validar(r);
        Tarefa t = buscarEntidade(id);
        TarefaSnapshot antes = TarefaSnapshot.de(t); // estado anterior, para o desfazer
        t.setTitulo(r.titulo().trim());
        t.setDescricao(limpar(r.descricao()));
        t.setPrioridade(r.prioridade() == null ? Prioridade.MEDIA : r.prioridade());
        t.setPrazo(r.prazo());
        tarefas.save(t); // UPDATE
        pilha.push(new AcaoEditarTarefa(antes));
        return TarefaResponse.de(t);
    }

    public TarefaResponse mudarStatus(Long id, StatusTarefa novoStatus) {
        if (novoStatus == null) throw new IllegalArgumentException("Status inválido.");
        Tarefa t = buscarEntidade(id);
        if (t.getStatus() == novoStatus) return TarefaResponse.de(t); // nada mudou: não empilha
        StatusTarefa anterior = t.getStatus();
        LocalDateTime conclusaoAnterior = t.getDataConclusao();
        t.mudarStatus(novoStatus);
        tarefas.save(t); // UPDATE imediato no banco
        pilha.push(new AcaoMoverTarefa(t.getTitulo(), id, anterior, conclusaoAnterior, novoStatus));
        return TarefaResponse.de(t);
    }

    // ===================== DELETE =====================

    public void excluir(Long id) {
        Tarefa t = buscarEntidade(id);
        List<TarefaSnapshot> fotos = new ArrayList<>();
        fotografar(t, fotos); // pai primeiro, depois as subtarefas
        removerEntidade(t);   // DELETE (cascade apaga as subtarefas)
        pilha.push(new AcaoExcluirTarefa(fotos));
    }

    // ===================== DESFAZER (usa a PILHA) =====================

    /** Desfaz a última ação. Retorna a descrição do que foi desfeito. */
    public String desfazer() {
        Acao topo = pilha.peek();                                   // PEEK: olha o topo sem remover
        if (topo == null) throw new IllegalStateException("Não há ações para desfazer.");
        try {
            topo.desfazer(this);                                    // polimorfismo: cada Acao sabe se reverter
        } finally {
            pilha.pop();                                            // POP: remove do topo (mesmo se falhar, para não travar a pilha)
        }
        return topo.getDescricao();
    }

    @Transactional(readOnly = true)
    public EstadoDesfazerResponse estadoDesfazer() {
        Acao topo = pilha.peek();
        return new EstadoDesfazerResponse(!pilha.estaVazia(), pilha.tamanho(), topo == null ? null : topo.getDescricao());
    }

    // ===== Operações de reversão (NÃO empilham nada) =====

    @Override
    public void reverterStatus(Long tarefaId, StatusTarefa statusAnterior, LocalDateTime conclusaoAnterior) {
        Tarefa t = buscarEntidade(tarefaId);
        t.restaurarStatus(statusAnterior, conclusaoAnterior);
        tarefas.save(t);
    }

    @Override
    public void reverterEdicao(TarefaSnapshot antes) {
        Tarefa t = buscarEntidade(antes.id());
        t.setTitulo(antes.titulo());
        t.setDescricao(antes.descricao());
        t.setPrioridade(antes.prioridade());
        t.setPrazo(antes.prazo());
        tarefas.save(t);
    }

    @Override
    public void reverterCriacao(List<Long> idsCriados) {
        for (Long id : idsCriados) {
            tarefas.findById(id).ifPresent(this::removerEntidade); // se já sumiu (apagada em cascata), ignora
        }
    }

    /**
     * Recria as tarefas apagadas COM OS MESMOS IDs (assim outras ações da pilha continuam válidas).
     * O JPA não permite escolher o id numa coluna autoincremento, então usamos JDBC direto.
     * Usa a mesma conexão/transação do Hibernate (session.doWork).
     */
    @Override
    public void reverterExclusao(List<TarefaSnapshot> excluidas) {
        String sql = "INSERT INTO tarefa (id, titulo, descricao, prioridade, status, data_criacao, prazo, "
                + "data_conclusao, estimativa, quadro_id, tarefa_pai_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        em.unwrap(Session.class).doWork(conexao -> {
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                for (TarefaSnapshot s : excluidas) { // o pai vem antes dos filhos
                    ps.setLong(1, s.id());
                    ps.setString(2, s.titulo());
                    ps.setString(3, s.descricao());
                    ps.setString(4, s.prioridade().name());
                    ps.setString(5, s.status().name());
                    ps.setString(6, s.dataCriacao().toString());
                    texto(ps, 7, s.prazo() == null ? null : s.prazo().toString());
                    texto(ps, 8, s.dataConclusao() == null ? null : s.dataConclusao().toString());
                    texto(ps, 9, s.estimativa());
                    ps.setLong(10, s.quadroId());
                    if (s.tarefaPaiId() == null) ps.setNull(11, Types.BIGINT); else ps.setLong(11, s.tarefaPaiId());
                    ps.executeUpdate();
                }
            }
        });
        em.clear(); // descarta o cache do Hibernate, que não sabe das linhas inseridas via JDBC
    }

    // ===================== auxiliares =====================

    private static void texto(PreparedStatement ps, int posicao, String valor) throws java.sql.SQLException {
        if (valor == null) ps.setNull(posicao, Types.VARCHAR); else ps.setString(posicao, valor);
    }

    private void fotografar(Tarefa t, List<TarefaSnapshot> destino) {
        destino.add(TarefaSnapshot.de(t));
        for (Tarefa sub : t.getSubtarefas()) fotografar(sub, destino);
    }

    /** Remove mantendo a coleção do pai coerente (necessário por causa do orphanRemoval/cascade). */
    private void removerEntidade(Tarefa t) {
        if (t.getTarefaPai() != null) {
            t.getTarefaPai().getSubtarefas().remove(t);
        } else {
            tarefas.delete(t);
        }
        tarefas.flush();
    }

    private Tarefa buscarEntidade(Long id) {
        return tarefas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa " + id + " não encontrada."));
    }

    private Quadro quadroPadrao() {
        return quadros.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Nenhum quadro cadastrado no banco."));
    }

    private void validar(TarefaRequest r) {
        if (r == null || r.titulo() == null || r.titulo().isBlank()) {
            throw new IllegalArgumentException("O título é obrigatório.");
        }
        if (r.titulo().trim().length() > 120) throw new IllegalArgumentException("O título deve ter no máximo 120 caracteres.");
        if (r.descricao() != null && r.descricao().length() > 2000) throw new IllegalArgumentException("A descrição deve ter no máximo 2000 caracteres.");
    }

    private String limpar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
