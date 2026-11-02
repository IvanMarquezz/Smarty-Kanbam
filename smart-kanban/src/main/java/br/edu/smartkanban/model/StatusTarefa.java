package br.edu.smartkanban.model;

/** Coluna do quadro Kanban em que a tarefa está. */
public enum StatusTarefa {
    A_FAZER,        // coluna "A FAZER" (TO DO)
    EM_ANDAMENTO,   // coluna "EM ANDAMENTO" (DOING)
    CONCLUIDA       // coluna "CONCLUÍDAS" (DONE)
}
