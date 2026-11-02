package br.edu.smartkanban.dto;

import br.edu.smartkanban.model.Prioridade;
import br.edu.smartkanban.model.StatusTarefa;
import br.edu.smartkanban.model.Tarefa;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados devolvidos ao frontend. Usamos um DTO em vez de expor a entidade JPA para
 * evitar loops de serialização (Tarefa -> subtarefas -> tarefaPai -> ...).
 */
public record TarefaResponse(
        Long id, String titulo, String descricao, Prioridade prioridade, StatusTarefa status,
        LocalDateTime dataCriacao, LocalDate prazo, LocalDateTime dataConclusao, String estimativa,
        Long tarefaPaiId, String tarefaPaiTitulo, int totalSubtarefas, int subtarefasConcluidas) {

    public static TarefaResponse de(Tarefa t) {
        Tarefa pai = t.getTarefaPai();
        int concluidas = (int) t.getSubtarefas().stream()
                .filter(s -> s.getStatus() == StatusTarefa.CONCLUIDA).count();
        return new TarefaResponse(
                t.getId(), t.getTitulo(), t.getDescricao(), t.getPrioridade(), t.getStatus(),
                t.getDataCriacao(), t.getPrazo(), t.getDataConclusao(), t.getEstimativa(),
                pai == null ? null : pai.getId(), pai == null ? null : pai.getTitulo(),
                t.getSubtarefas().size(), concluidas);
    }
}
