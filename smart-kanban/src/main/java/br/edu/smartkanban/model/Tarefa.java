package br.edu.smartkanban.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarefa do Kanban. Uma SUBTAREFA é simplesmente uma Tarefa com "tarefaPai" preenchido
 * (auto-relacionamento). Não criamos uma classe Subtarefa herdando de Tarefa porque ela não
 * teria comportamento diferente: seria herança artificial.
 */
@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade = Prioridade.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTarefa status = StatusTarefa.A_FAZER;

    @Convert(converter = DataConverters.LocalDateTimeConverter.class)
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Convert(converter = DataConverters.LocalDateConverter.class)
    private LocalDate prazo;

    @Convert(converter = DataConverters.LocalDateTimeConverter.class)
    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    /** Estimativa de tempo sugerida pela IA (ex.: "2 horas"). Opcional. */
    private String estimativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadro_id", nullable = false)
    private Quadro quadro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_pai_id")
    private Tarefa tarefaPai;

    @OneToMany(mappedBy = "tarefaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tarefa> subtarefas = new ArrayList<>();

    protected Tarefa() { }

    public Tarefa(String titulo, String descricao, Prioridade prioridade, LocalDate prazo, Quadro quadro) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.prazo = prazo;
        this.quadro = quadro;
    }

    /**
     * Regra de negócio encapsulada: ao mover para CONCLUIDA registra a data de conclusão;
     * ao sair de CONCLUIDA, limpa essa data. Quem chama não precisa conhecer essa regra.
     */
    public void mudarStatus(StatusTarefa novo) {
        this.status = novo;
        this.dataConclusao = (novo == StatusTarefa.CONCLUIDA) ? LocalDateTime.now() : null;
    }

    /** Usado pelo "desfazer": devolve exatamente o status e a data de conclusão anteriores. */
    public void restaurarStatus(StatusTarefa statusAnterior, LocalDateTime conclusaoAnterior) {
        this.status = statusAnterior;
        this.dataConclusao = conclusaoAnterior;
    }

    /** Vincula uma subtarefa a esta tarefa (mantém os dois lados da relação). */
    public void adicionarSubtarefa(Tarefa sub) {
        sub.tarefaPai = this;
        sub.quadro = this.quadro;
        subtarefas.add(sub);
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }
    public StatusTarefa getStatus() { return status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public String getEstimativa() { return estimativa; }
    public void setEstimativa(String estimativa) { this.estimativa = estimativa; }
    public Quadro getQuadro() { return quadro; }
    public Tarefa getTarefaPai() { return tarefaPai; }
    public List<Tarefa> getSubtarefas() { return subtarefas; }
}
