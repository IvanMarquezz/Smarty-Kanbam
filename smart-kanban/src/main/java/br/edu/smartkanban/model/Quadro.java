package br.edu.smartkanban.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Quadro Kanban: pertence a um Usuario e contém várias Tarefas (composição). */
@Entity
@Table(name = "quadro")
public class Quadro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "quadro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tarefa> tarefas = new ArrayList<>();

    protected Quadro() { }

    public Quadro(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Usuario getUsuario() { return usuario; }
    void setUsuario(Usuario usuario) { this.usuario = usuario; } // só Usuario altera (encapsulamento)
    public List<Tarefa> getTarefas() { return tarefas; }
}
