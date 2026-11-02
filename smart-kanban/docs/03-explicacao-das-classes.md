# C. Explicação das classes

Pacote base: `br.edu.smartkanban`.

## model/: entidades (viram tabelas)

### `Tarefa`
- **Responsabilidade:** representar uma tarefa (ou subtarefa) do Kanban.
- **Atributos:** `id`, `titulo`, `descricao`, `prioridade` (enum), `status` (enum), `dataCriacao`, `prazo`, `dataConclusao`, `estimativa`, `quadro`, `tarefaPai`, `subtarefas`.
- **Métodos principais:** `mudarStatus(novo)` (regra de data de conclusão), `restaurarStatus(...)` (usado pelo desfazer), `adicionarSubtarefa(sub)` (mantém os dois lados da relação), getters/setters controlados (não há `setStatus` nem `setDataConclusao` públicos).
- **Relacionamentos:** N:1 com `Quadro`; auto-relacionamento (`tarefaPai` N:1 / `subtarefas` 1:N, com `cascade ALL` e `orphanRemoval`).

### `Quadro`
- Agrupa tarefas e pertence a um usuário. **Atributos:** `id`, `nome`, `descricao`, `usuario`, `tarefas`. `setUsuario` é package-private.

### `Usuario`
- Dono dos quadros. **Atributos:** `id`, `nome`, `email`, `senha` (hash SHA-256). **Método:** `adicionarQuadro`.

### `Prioridade` / `StatusTarefa`
- Enums: `BAIXA/MEDIA/ALTA` e `A_FAZER/EM_ANDAMENTO/CONCLUIDA`.

### `DataConverters`
- Conversores JPA `LocalDate`/`LocalDateTime` ⇄ texto ISO para o SQLite.

## repository/: acesso ao banco
- `TarefaRepository`, `QuadroRepository`, `UsuarioRepository`: **interfaces** que estendem `JpaRepository`; o Spring cria a implementação. Têm o CRUD pronto e consultas derivadas pelo nome do método.

## estrutura/: pilha e ações (Pilares 1 e 4)

| Classe | Responsabilidade |
|---|---|
| `PilhaDeAcoes` | A pilha LIFO. Atributos privados: `elementos` (`ArrayDeque<Acao>`), `capacidadeMaxima`. Métodos: `push`, `pop`, `peek`, `estaVazia`, `tamanho`. |
| `Acao` (abstrata) | Molde de qualquer ação desfazível: `descricao`, `momento`, método abstrato `desfazer(OperacoesDeDesfazer)`. |
| `AcaoMoverTarefa` | Guarda `tarefaId`, `statusAnterior`, `conclusaoAnterior`. Desfazer → `reverterStatus`. |
| `AcaoEditarTarefa` | Guarda o `TarefaSnapshot` anterior. Desfazer → `reverterEdicao`. |
| `AcaoExcluirTarefa` | Guarda lista de snapshots (tarefa + subtarefas). Desfazer → `reverterExclusao`. |
| `AcaoCriarTarefa` | Guarda os ids criados. Desfazer → `reverterCriacao` (exclui). |
| `TarefaSnapshot` | `record` imutável com todos os campos de uma tarefa num instante. |
| `OperacoesDeDesfazer` | Interface com as 4 reversões; implementada por `GerenciadorDeTarefas`. |

Relações: as 4 ações **herdam** de `Acao`; a pilha **contém** `Acao`s (agregação por tipo-base = polimorfismo); `Acao` **depende da interface**, não do gerenciador.

## service/: regras de negócio

### `GerenciadorDeTarefas` (implementa `OperacoesDeDesfazer`)
- Faz o CRUD e controla a pilha. **Atributos:** `tarefas` e `quadros` (repositórios), `em` (EntityManager) e `pilha` (composição).
- **Métodos:** `listar`, `buscar`, `criar`, `criarSubtarefas`, `editar`, `mudarStatus`, `excluir`, `desfazer`, `estadoDesfazer` e as quatro `reverter*`. Auxiliares privados: `validar`, `fotografar`, `removerEntidade`, `buscarEntidade`, `quadroPadrao`.

### `ServicoIA`
- Cliente da API Gemini. **Atributos:** `apiKey`, `modelo`, `timeout`, `http` (`HttpClient`), `mapper` (Jackson).
- **Métodos:** `estaConfigurada`, `planejar` (público); `montarCorpo`, `traduzirErroHttp`, `interpretarResposta` (privados).

## controller/: API REST
- `TarefaController` (`/api/tarefas`), `DesfazerController` (`/api/desfazer`), `IAController` (`/api/ia`). Só recebem HTTP, chamam o service e devolvem JSON; **sem regra de negócio**.

## dto/: objetos de transporte
- `TarefaRequest`, `StatusRequest`, `PlanejarRequest`, `AceitarSubtarefasRequest` (entrada) e `TarefaResponse`, `SubtarefaSugeridaDTO`, `EstadoDesfazerResponse` (saída). Evitam expor entidades JPA (que causariam laços infinitos ao serializar tarefa ⇄ subtarefas) e deixam o contrato da API explícito.

## excecao/ e config/
- `RecursoNaoEncontradoException` (404), `ServicoIAException` (carrega o status HTTP), `ApiExceptionHandler` (`@RestControllerAdvice`: transforma exceções em `{"erro": "..."}`).
- `DataInitializer` (`CommandLineRunner`): cria usuário e quadro padrão se o banco estiver vazio.

## Diagrama resumido

```
Usuario 1──* Quadro 1──* Tarefa *──1 (tarefaPai) Tarefa

Acao (abstrata)
 ├─ AcaoMoverTarefa
 ├─ AcaoEditarTarefa
 ├─ AcaoExcluirTarefa
 └─ AcaoCriarTarefa
PilhaDeAcoes ◆── Acao*          GerenciadorDeTarefas ◆── PilhaDeAcoes
GerenciadorDeTarefas ──implements──► OperacoesDeDesfazer ◄──usa── Acao
```
