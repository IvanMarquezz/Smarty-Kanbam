package br.edu.smartkanban.dto;

import java.util.List;

public record AceitarSubtarefasRequest(Long tarefaPaiId, List<SubtarefaSugeridaDTO> subtarefas) { }
