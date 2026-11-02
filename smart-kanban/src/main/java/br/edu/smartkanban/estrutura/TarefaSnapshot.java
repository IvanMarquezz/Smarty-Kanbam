package br.edu.smartkanban.estrutura;

import br.edu.smartkanban.model.Prioridade;
import br.edu.smartkanban.model.StatusTarefa;
import br.edu.smartkanban.model.Tarefa;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * "Fotografia" imutável de uma tarefa num instante. É o que cada Acao guarda para
 * conseguir restaurar o estado anterior (record = classe imutável, sem setters).
 */
public record TarefaSnapshot(
        Long id, String titulo, String descricao, Prioridade prioridade, StatusTarefa status,
        LocalDateTime dataCriacao, LocalDate prazo, LocalDateTime dataConclusao, String estimativa,
        Long quadroId, Long tarefaPaiId) {

    public static TarefaSnapshot de(Tarefa t) {
        return new TarefaSnapshot(
                t.getId(), t.getTitulo(), t.getDescricao(), t.getPrioridade(), t.getStatus(),
                t.getDataCriacao(), t.getPrazo(), t.getDataConclusao(), t.getEstimativa(),
                t.getQuadro().getId(), t.getTarefaPai() == null ? null : t.getTarefaPai().getId());
    }
}
