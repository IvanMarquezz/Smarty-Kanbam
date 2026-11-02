package br.edu.smartkanban.dto;

import br.edu.smartkanban.model.Prioridade;
import java.time.LocalDate;

/** Dados enviados pelo frontend para criar/editar uma tarefa. */
public record TarefaRequest(String titulo, String descricao, Prioridade prioridade, LocalDate prazo) { }
