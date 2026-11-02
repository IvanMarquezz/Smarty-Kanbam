package br.edu.smartkanban.estrutura;

import java.util.List;

/** Criação de uma ou mais tarefas (inclui subtarefas aceitas da IA). Desfazer = excluir. */
public class AcaoCriarTarefa extends Acao {
    private final List<Long> idsCriados;

    public AcaoCriarTarefa(String descricao, List<Long> idsCriados) {
        super(descricao);
        this.idsCriados = List.copyOf(idsCriados);
    }

    @Override
    public void desfazer(OperacoesDeDesfazer operacoes) {
        operacoes.reverterCriacao(idsCriados);
    }
}
