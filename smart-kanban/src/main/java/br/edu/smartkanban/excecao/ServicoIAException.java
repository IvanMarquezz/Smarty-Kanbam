package br.edu.smartkanban.excecao;

import org.springframework.http.HttpStatus;

/** Qualquer falha na integração com a IA, com uma mensagem clara para o usuário. */
public class ServicoIAException extends RuntimeException {
    private final HttpStatus status;

    public ServicoIAException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
