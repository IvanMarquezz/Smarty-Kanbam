package br.edu.smartkanban.dto;

/** Uma subtarefa sugerida pela IA (ainda NÃO salva no banco). */
public record SubtarefaSugeridaDTO(String titulo, String descricao, String estimativa) { }
