# D. Estrutura de dados: a Pilha (Stack)

## O que é
Uma **pilha** é uma coleção em que só se mexe em **uma ponta, o topo**. Funciona como uma pilha de pratos: você coloca e retira sempre no topo. Regra **LIFO** (*Last In, First Out*): **o último a entrar é o primeiro a sair**. É exatamente o que o "desfazer" precisa: a última coisa que você fez é a primeira a ser desfeita.

## As três operações

| Operação | Função | No projeto | Custo |
|---|---|---|---|
| **PUSH** | Coloca um elemento **no topo** | `elementos.addFirst(acao)` | O(1) |
| **POP** | **Remove e devolve** o elemento do topo | `elementos.removeFirst()` | O(1) |
| **PEEK** | **Olha** o topo **sem remover** | `elementos.peekFirst()` | O(1) |

Diferença-chave: **pop muda a pilha; peek não muda**.

## O código (resumo de `PilhaDeAcoes.java`)

```java
private final Deque<Acao> elementos = new ArrayDeque<>();   // privado: encapsulamento

public synchronized void push(Acao acao) {
    elementos.addFirst(acao);                                // entra no topo
    if (elementos.size() > capacidadeMaxima) elementos.removeLast(); // descarta a mais antiga
}
public synchronized Acao pop()  { if (vazia) throw new NoSuchElementException(...); return elementos.removeFirst(); }
public synchronized Acao peek() { return elementos.peekFirst(); }   // null se vazia
```

- **Por que `ArrayDeque`?** É a implementação recomendada pela documentação do Java para pilhas (a classe `Stack` é antiga e sincronizada à toa). Usamos só a ponta da frente, então ele se comporta como pilha.
- **Por que `synchronized`?** Duas requisições HTTP simultâneas poderiam mexer na pilha ao mesmo tempo.
- **Por que capacidade 50?** Para a memória não crescer sem limite. Ao estourar, remove-se a **base** (a mais antiga).

## Como a pilha é usada no sistema

**Empilhar** (em `GerenciadorDeTarefas`), sempre depois de alterar o banco:

```java
// mudarStatus
pilha.push(new AcaoMoverTarefa(titulo, id, statusAnterior, conclusaoAnterior, novoStatus));
// editar:    pilha.push(new AcaoEditarTarefa(snapshotAntes));
// excluir:   pilha.push(new AcaoExcluirTarefa(snapshotsDaTarefaESubtarefas));
// criar:     pilha.push(new AcaoCriarTarefa(descricao, idsCriados));
```

**Desfazer** (o "Ctrl+Z" do servidor):

```java
public String desfazer() {
    Acao topo = pilha.peek();                       // 1) PEEK: quem está no topo?
    if (topo == null) throw new IllegalStateException("Não há ações para desfazer.");
    try {
        topo.desfazer(this);                        // 2) reverte no BANCO (polimorfismo)
    } finally {
        pilha.pop();                                // 3) POP: tira do topo
    }
    return topo.getDescricao();
}
```

O `peek` também alimenta o botão: `GET /api/desfazer/estado` devolve a descrição do topo e o tamanho, e a tela mostra "Próxima ação a desfazer: ...".

## Exemplo real, passo a passo

Estado inicial: pilha vazia, banco sem tarefas. (O topo é o primeiro da lista.)

| # | Ação do usuário | Banco | Pilha depois (topo → base) |
|---|---|---|---|
| 1 | Cria "Estudar Pilhas" (id 1) | INSERT | `[Criar "Estudar Pilhas"]` |
| 2 | Move para EM ANDAMENTO | UPDATE status | `[Mover → EM_ANDAMENTO, Criar]` |
| 3 | Edita o título para "Estudar Pilhas e Filas" | UPDATE titulo | `[Editar, Mover, Criar]` |
| 4 | Exclui a tarefa | DELETE | `[Excluir, Editar, Mover, Criar]` |
| 5 | **Ctrl+Z** | **restaura** a tarefa (mesmo id 1) | `[Editar, Mover, Criar]` |
| 6 | **Ctrl+Z** | título volta a "Estudar Pilhas" | `[Mover, Criar]` |
| 7 | **Ctrl+Z** | status volta a A_FAZER | `[Criar]` |
| 8 | **Ctrl+Z** | tarefa é removida (desfez a criação) | `[]` (vazia) |
| 9 | **Ctrl+Z** | erro 409 "Não há ações para desfazer." | `[]` |

Note a ordem **inversa** das ações: isso é o LIFO em ação.

## Por que cada Acao guarda o que guarda?
Para desfazer é preciso saber **como era antes**:
- Mover → status e data de conclusão anteriores.
- Editar → *snapshot* (cópia imutável) completo da tarefa antes da edição.
- Excluir → snapshots da tarefa **e das subtarefas**, pois apagar o pai apaga os filhos.
- Criar → os ids criados (desfazer = excluí-los).

## Detalhes que o professor pode explorar
- **Reverter não empilha:** os métodos `reverter*` não chamam `push`; senão o desfazer geraria novas ações e nunca terminaria (não existe "refazer" neste projeto).
- **Pilha em memória:** reiniciar o servidor esvazia o histórico; as tarefas continuam no banco. Para persistir a pilha seria preciso uma tabela de ações.
- **Uma pilha global:** como o sistema tem um único usuário/quadro, há uma pilha única. Com vários usuários seria uma pilha por usuário (ex.: `Map<Long, PilhaDeAcoes>`).
- **Teste:** a lógica da pilha foi verificada com um teste que confirma LIFO, peek sem remoção, `pop` em pilha vazia e descarte da ação mais antiga ao atingir a capacidade.
