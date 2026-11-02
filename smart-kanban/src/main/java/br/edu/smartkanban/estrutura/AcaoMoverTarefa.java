package br.edu.smartkanban.estrutura;

import br.edu.smartkanban.model.StatusTarefa;
import java.time.LocalDateTime;

/** Mudança de coluna. Guarda o status (e a data de conclusão) ANTERIORES. */
public class AcaoMoverTarefa extends Acao {
    private final Long tarefaId;
    private final StatusTarefa statusAnterior;
    private final LocalDateTime conclusaoAnterior;

    public AcaoMoverTarefa(String titulo, Long tarefaId, StatusTarefa statusAnterior,
                           LocalDateTime conclusaoAnterior, StatusTarefa statusNovo) {
        super("Mover \"" + titulo + "\" para " + statusNovo);
        this.tarefaId = tarefaId;
        this.statusAnterior = statusAnterior;
        this.conclusaoAnterior = conclusaoAnterior;
    }

    @Override
    public void desfazer(OperacoesDeDesfazer operacoes) {
        operacoes.reverterStatus(tarefaId, statusAnterior, conclusaoAnterior);
    }
}
