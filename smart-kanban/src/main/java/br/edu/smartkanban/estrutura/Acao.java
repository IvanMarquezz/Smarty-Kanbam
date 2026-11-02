package br.edu.smartkanban.estrutura;

import java.time.LocalDateTime;

/**
 * Classe ABSTRATA: representa "algo que o usuário fez e que pode ser desfeito".
 * Cada subclasse sabe como reverter a SUA operação (polimorfismo): a pilha guarda
 * objetos do tipo Acao e chama desfazer() sem precisar saber qual é a subclasse.
 */
public abstract class Acao {
    private final String descricao;
    private final LocalDateTime momento = LocalDateTime.now();

    protected Acao(String descricao) { this.descricao = descricao; }

    /** Reverte a operação atualizando o banco (via OperacoesDeDesfazer). */
    public abstract void desfazer(OperacoesDeDesfazer operacoes);

    public String getDescricao() { return descricao; }
    public LocalDateTime getMomento() { return momento; }
}
