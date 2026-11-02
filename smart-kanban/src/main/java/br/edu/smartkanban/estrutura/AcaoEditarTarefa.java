package br.edu.smartkanban.estrutura;

/** Edição de campos. Guarda o snapshot da tarefa ANTES de editar. */
public class AcaoEditarTarefa extends Acao {
    private final TarefaSnapshot antes;

    public AcaoEditarTarefa(TarefaSnapshot antes) {
        super("Editar \"" + antes.titulo() + "\"");
        this.antes = antes;
    }

    @Override
    public void desfazer(OperacoesDeDesfazer operacoes) {
        operacoes.reverterEdicao(antes);
    }
}
