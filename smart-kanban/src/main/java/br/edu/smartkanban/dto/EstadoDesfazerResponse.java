package br.edu.smartkanban.dto;

/** Estado da pilha para o botão "Desfazer": há algo a desfazer? o que seria desfeito (peek)? */
public record EstadoDesfazerResponse(boolean podeDesfazer, int quantidade, String proximaAcao) { }
