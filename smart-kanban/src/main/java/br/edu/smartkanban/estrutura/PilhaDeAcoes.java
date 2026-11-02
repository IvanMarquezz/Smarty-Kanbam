package br.edu.smartkanban.estrutura;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

/**
 * PILHA (STACK) de ações - estrutura de dados do tipo LIFO (Last In, First Out):
 * a última ação empilhada é a primeira a ser desfeita.
 *
 * Implementação: um ArrayDeque usado SOMENTE por uma ponta (o "topo" = início do deque).
 * O ArrayDeque fica privado (encapsulamento): quem usa a pilha só enxerga push/pop/peek.
 *
 * Complexidade: push, pop e peek são O(1).
 * Os métodos são synchronized porque várias requisições HTTP podem chegar ao mesmo tempo.
 */
public class PilhaDeAcoes {

    private final Deque<Acao> elementos = new ArrayDeque<>();
    private final int capacidadeMaxima;

    public PilhaDeAcoes(int capacidadeMaxima) {
        if (capacidadeMaxima < 1) throw new IllegalArgumentException("Capacidade deve ser >= 1");
        this.capacidadeMaxima = capacidadeMaxima;
    }

    /** PUSH: coloca uma ação NO TOPO da pilha. Se estourar a capacidade, descarta a mais ANTIGA (base). */
    public synchronized void push(Acao acao) {
        if (acao == null) throw new IllegalArgumentException("Ação não pode ser nula");
        elementos.addFirst(acao);
        if (elementos.size() > capacidadeMaxima) {
            elementos.removeLast(); // remove a da base (a mais antiga)
        }
    }

    /** POP: REMOVE e DEVOLVE a ação do topo. Lança exceção se a pilha estiver vazia. */
    public synchronized Acao pop() {
        if (elementos.isEmpty()) throw new NoSuchElementException("A pilha está vazia");
        return elementos.removeFirst();
    }

    /** PEEK: apenas ESPIA o topo, SEM remover. Devolve null se a pilha estiver vazia. */
    public synchronized Acao peek() {
        return elementos.peekFirst();
    }

    public synchronized boolean estaVazia() { return elementos.isEmpty(); }

    public synchronized int tamanho() { return elementos.size(); }
}
