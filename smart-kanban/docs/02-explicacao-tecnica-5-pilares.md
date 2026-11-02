# B. Explicação técnica: como cada pilar foi implementado

## Visão geral do fluxo (exemplo: arrastar um cartão para "Concluídas")

```
app.js (navegador)  ──PATCH /api/tarefas/7/status {"status":"CONCLUIDA"}──►  TarefaController
      TarefaController ─► GerenciadorDeTarefas.mudarStatus(7, CONCLUIDA)
            1. lê a Tarefa no banco (TarefaRepository.findById)         ← Pilar 3
            2. guarda o status anterior                                  ← para o desfazer
            3. tarefa.mudarStatus(...)  (regra encapsulada)              ← Pilar 4
            4. tarefaRepository.save() → UPDATE no SQLite                ← Pilares 2 e 3
            5. pilha.push(new AcaoMoverTarefa(...))                      ← Pilar 1
      resposta JSON ─► navegador redesenha o quadro
```

Camadas: **controller** (HTTP) → **service** (regras) → **repository** (SQL via JPA) → **banco**. Cada camada só conversa com a vizinha.

---

## Pilar 1: Estruturas de dados básicas (Pilha)

Arquivo: `estrutura/PilhaDeAcoes.java`.

- Pilha **LIFO** implementada sobre um `ArrayDeque<Acao>`, usado só por uma ponta (o topo).
- `push(acao)`: empilha no topo. `pop()`: remove e devolve o topo. `peek()`: olha o topo sem remover. Todos O(1).
- **Uso real:** `GerenciadorDeTarefas` empilha uma `Acao` em toda criação, edição, mudança de status e exclusão; `desfazer()` faz `peek` → `acao.desfazer()` → `pop`.
- Capacidade máxima de 50 ações: ao estourar, a mais antiga (base) é descartada.
- Detalhes e exemplos passo a passo: [04-estrutura-de-dados-pilha.md](04-estrutura-de-dados-pilha.md).

## Pilar 2: Banco de dados (modelagem e persistência)

- **SQLite** em arquivo (`smartkanban.db`): banco real, sem servidor, ideal para projetos acadêmicos.
- Três tabelas e seus relacionamentos:

```
usuario (1) ───< quadro (1) ───< tarefa
                                   └─ tarefa_pai_id → tarefa.id   (auto-relacionamento: subtarefas)
```

- `Tarefa` tem: `id, titulo, descricao, prioridade, status, data_criacao, prazo, data_conclusao, quadro_id, tarefa_pai_id` (+ `estimativa`, preenchida pela IA).
- **Mapeamento objeto-relacional (ORM):** as anotações `@Entity`, `@Id`, `@ManyToOne`, `@OneToMany` dizem ao Hibernate como cada classe vira tabela. Com `ddl-auto=update` ele cria as tabelas na primeira execução e mantém os dados nas seguintes.
- `prioridade` e `status` são enums gravados como texto (`@Enumerated(STRING)`), o que deixa o banco legível.
- Datas são gravadas como texto ISO-8601 por conversores (`DataConverters`), pois o SQLite não tem tipo de data nativo.
- `script-banco.sql` mostra o mesmo modelo em SQL puro, para estudo.
- **Persistência:** os dados vivem no arquivo `.db`; fechar e reabrir o sistema não os perde. (Só a *pilha de desfazer* é em memória.)

## Pilar 3: Database Application (conexão e CRUD)

- **Conexão:** configurada em `application.properties` (`jdbc:sqlite:smartkanban.db`). O Spring cria o pool de conexões (limitado a 1, pois o SQLite aceita um escritor por vez).
- **Repositórios:** `TarefaRepository extends JpaRepository<Tarefa, Long>`. O Spring Data gera sozinho `save`, `findById`, `findAll`, `delete`... e também consultas pelo nome do método (`findByQuadroIdOrderByIdAsc`).
- **CRUD completo** em `GerenciadorDeTarefas`:

| Operação | Método | SQL gerado (resumo) | Rota |
|---|---|---|---|
| CREATE | `criar`, `criarSubtarefas` | `INSERT INTO tarefa ...` | `POST /api/tarefas` |
| READ | `listar`, `buscar` | `SELECT ... FROM tarefa ...` | `GET /api/tarefas[/{id}]` |
| UPDATE | `editar`, `mudarStatus` | `UPDATE tarefa SET ... WHERE id=?` | `PUT` / `PATCH` |
| DELETE | `excluir` | `DELETE FROM tarefa WHERE id=?` | `DELETE /api/tarefas/{id}` |

- `@Transactional`: cada operação é uma transação; se algo falhar no meio, o banco volta ao estado anterior (rollback).
- **Desfazer de exclusão:** reinsere as linhas com os **mesmos ids** via JDBC (`Session.doWork`), pois o JPA não permite escolher o id de uma coluna autoincremento. Assim, outras ações já na pilha continuam apontando para tarefas válidas.

## Pilar 4: Programação Orientada a Objetos

| Princípio | Onde está |
|---|---|
| **Encapsulamento** | Atributos `private` em todas as classes. `PilhaDeAcoes` esconde o `ArrayDeque`. `Tarefa.mudarStatus()` guarda a regra "ao concluir, registrar data; ao reabrir, limpar". `Quadro.setUsuario` é package-private: só `Usuario.adicionarQuadro` o usa. |
| **Herança** | `AcaoMoverTarefa`, `AcaoEditarTarefa`, `AcaoExcluirTarefa`, `AcaoCriarTarefa` estendem `Acao` (classe abstrata). Faz sentido porque as 4 *são* ações e compartilham descrição e momento. |
| **Polimorfismo** | `Acao.desfazer()` é abstrato; cada subclasse o implementa de um jeito. A pilha e o gerenciador chamam `topo.desfazer(this)` sem saber o tipo concreto. |
| **Abstração / interfaces** | `OperacoesDeDesfazer` é o contrato que as ações usam; `GerenciadorDeTarefas` o implementa. As ações não dependem da classe concreta. |
| **Composição** | `Usuario` possui `Quadro`s; `Quadro` possui `Tarefa`s; `Tarefa` possui `subtarefas`; `GerenciadorDeTarefas` possui uma `PilhaDeAcoes`. |
| **Separação de responsabilidades** | Controller só fala HTTP; Service só regras; Repository só banco; `ServicoIA` só a API externa; DTOs só transporte de dados. |
| **Imutabilidade** | `TarefaSnapshot` e os DTOs são `record`s (sem setters). |

**Decisão de modelagem (importante na defesa):** *não* criei a classe `Subtarefa extends Tarefa`. Uma subtarefa não tem atributo nem comportamento novo: é uma `Tarefa` com `tarefaPai` preenchido. Criar a subclasse seria herança artificial só para "cumprir requisito".

## Pilar 5: Inteligência Artificial (integração com API)

Arquivos: `service/ServicoIA.java`, `controller/IAController.java`, `static/js/app.js` (modal de sugestões).

1. O usuário clica em ✨. O navegador envia título e descrição para `POST /api/ia/planejar` (do **nosso** backend).
2. `ServicoIA` monta um prompt em português e chama, via HTTPS, o endpoint real `generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent`, com a chave no cabeçalho `x-goog-api-key`.
3. Pedimos **resposta estruturada**: `responseMimeType: application/json` + `responseSchema` (lista de objetos com `titulo`, `descricao`, `estimativa`). Assim não precisamos "adivinhar" texto livre.
4. O backend lê `candidates[0].content.parts[].text`, converte o JSON em `SubtarefaSugeridaDTO` com o Jackson e devolve ao navegador. **Nada é gravado ainda.**
5. A interface mostra as sugestões com caixas de seleção. O usuário marca as aceitas e clica em *Salvar selecionadas* → `POST /api/ia/aceitar` cria cada uma como `Tarefa` com `tarefa_pai_id` preenchido. Descartar apenas fecha o modal.
6. Salvar as subtarefas empilha **uma** `AcaoCriarTarefa`; um Ctrl+Z remove todas de uma vez.

**Segurança da chave:** vem da variável de ambiente `GEMINI_API_KEY` (lida em `application.properties`); só o backend a usa; o endpoint `/api/ia/status` informa apenas *se* está configurada, nunca o valor.

**Tratamento de erros (sempre com mensagem clara, sem simular sucesso):**

| Situação | Resposta ao usuário |
|---|---|
| Sem chave | 503: "A chave da API de IA não está configurada..." |
| Sem internet / DNS | 502: "Não foi possível conectar à API de IA..." |
| Demora demais (30 s) | 504: "A IA demorou demais para responder..." |
| Chave inválida (400/403) | mensagem específica |
| Modelo inexistente (404) | orienta ajustar `GEMINI_MODEL` |
| Cota excedida (429) | "Limite de uso gratuito atingido..." |
| API fora do ar (5xx) | "A API de IA está indisponível..." |
| JSON fora do formato / vazio | "A IA devolveu uma resposta fora do formato..." |
