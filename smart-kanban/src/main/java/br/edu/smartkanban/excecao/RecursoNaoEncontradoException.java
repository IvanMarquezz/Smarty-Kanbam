package br.edu.smartkanban.excecao;

/** Lançada quando uma tarefa pedida não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
