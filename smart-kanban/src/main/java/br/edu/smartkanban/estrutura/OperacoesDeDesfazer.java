package br.edu.smartkanban.estrutura;

import java.time.LocalDateTime;
import java.util.List;
import br.edu.smartkanban.model.StatusTarefa;

/**
 * Contrato das operações de "reversão". As Acoes dependem desta INTERFACE (e não da classe
 * concreta GerenciadorDeTarefas), o que reduz o acoplamento. Estes métodos NÃO empilham
 * nada na pilha: desfazer não pode gerar novas ações desfazíveis.
 */
public interface OperacoesDeDesfazer {
    void reverterStatus(Long tarefaId, StatusTarefa statusAnterior, LocalDateTime conclusaoAnterior);
    void reverterEdicao(TarefaSnapshot antes);
    void reverterExclusao(List<TarefaSnapshot> tarefasExcluidas);
    void reverterCriacao(List<Long> idsCriados);
}
