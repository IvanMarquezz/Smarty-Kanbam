package br.edu.smartkanban.estrutura;

import java.util.List;

/**
 * Exclusão. Guarda snapshots da tarefa E de suas subtarefas (a tarefa vem primeiro na lista),
 * pois excluir a tarefa-pai também apaga as subtarefas.
 */
public class AcaoExcluirTarefa extends Acao {
    private final List<TarefaSnapshot> excluidas;

    public AcaoExcluirTarefa(List<TarefaSnapshot> excluidas) {
        super("Excluir \"" + excluidas.get(0).titulo() + "\""
                + (excluidas.size() > 1 ? " (+" + (excluidas.size() - 1) + " subtarefas)" : ""));
        this.excluidas = List.copyOf(excluidas);
    }

    @Override
    public void desfazer(OperacoesDeDesfazer operacoes) {
        operacoes.reverterExclusao(excluidas);
    }
}
