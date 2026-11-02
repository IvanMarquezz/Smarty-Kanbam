package br.edu.smartkanban.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Dono dos quadros. Relação 1:N com Quadro (composição: o usuário "possui" seus quadros). */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    /** Guardamos apenas o HASH da senha (nunca o texto puro). */
    @Column(nullable = false)
    private String senha;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quadro> quadros = new ArrayList<>();

    protected Usuario() { } // exigido pelo JPA

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senha = senhaHash;
    }

    /** Mantém os dois lados da relação coerentes. */
    public void adicionarQuadro(Quadro quadro) {
        quadro.setUsuario(this);
        quadros.add(quadro);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public List<Quadro> getQuadros() { return quadros; }
}
