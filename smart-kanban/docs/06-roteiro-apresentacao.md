# F. Roteiro de apresentação (≈ 10 a 12 minutos)

**Antes de começar:** servidor rodando, `.env` com a chave, navegador em <http://localhost:8080>, quadro vazio (apague `smartkanban.db` se preciso). Deixe o código aberto no IDE nos arquivos: `PilhaDeAcoes`, `Acao`, `GerenciadorDeTarefas`, `ServicoIA`.

## 1. Abertura (1 min)
"Apresento o Smart Kanban, gerenciador de tarefas inspirado no Kanban. Ele integra cinco pilares: estruturas de dados, banco de dados, CRUD, POO e IA. Vou demonstrar cada um funcionando e mostrar onde estão no código."

## 2. Pilares 2 e 3: banco e CRUD (2 min)
1. **Criar** uma tarefa: "Desenvolver sistema de alunos", prioridade Alta, com prazo.
2. **Editar** o título; abrir **Detalhes** (🔍).
3. Mostrar a cor da prioridade e os contadores das colunas.
4. Em outra aba: `/api/tarefas` → JSON vindo do banco.
5. 📌 *Falar:* "Cada ação passa por Controller → Service → Repository → SQLite. O banco é o arquivo `smartkanban.db`." Mostrar `Tarefa.java` (anotações) e `TarefaRepository`.
6. **Persistência:** parar e reiniciar o servidor → a tarefa continua lá.

## 3. Quadro Kanban (1 min)
Arrastar o cartão de **A FAZER** para **EM ANDAMENTO** e depois **CONCLUÍDAS** (e usar ◀ ▶ uma vez). 📌 "A mudança de status faz UPDATE imediato no banco." Recarregar (F5) para provar.

## 4. Pilar 1: a Pilha e o Ctrl+Z (3 min), o ponto central
1. Observar o texto "Próxima ação a desfazer (topo da pilha)" e o contador do botão.
2. Fazer em sequência: criar tarefa → mover → editar → excluir.
3. Apertar **Ctrl+Z** repetidamente: a tarefa volta, a edição some, o status volta, a criação é desfeita. 📌 "Ordem inversa: isso é LIFO."
4. Apertar de novo com a pilha vazia → mensagem "Não há ações para desfazer".
5. **Mostrar o código** `PilhaDeAcoes.java`: `push`, `pop`, `peek` (O(1), `ArrayDeque`, encapsulado). Depois `GerenciadorDeTarefas.desfazer()`: peek → `desfazer()` → pop.

## 5. Pilar 4: POO (1,5 min)
Mostrar `Acao` (abstrata) e uma subclasse, por exemplo `AcaoMoverTarefa`. 📌 "Polimorfismo: o gerenciador chama `topo.desfazer()` sem saber o tipo. Encapsulamento: a pilha esconde o `ArrayDeque` e `Tarefa.mudarStatus()` guarda a regra da data. Composição: Usuário → Quadro → Tarefa. Não criei `Subtarefa extends Tarefa` porque seria herança artificial."

## 6. Pilar 5: IA (2 min)
1. Clicar em ✨ na tarefa "Desenvolver sistema de alunos".
2. Mostrar o estado de carregamento e as subtarefas com **estimativa de tempo**.
3. Desmarcar uma, **Salvar selecionadas** → aparecem como cartões "↳ de: ..." e o contador de subtarefas no cartão pai.
4. **Ctrl+Z** → todas as subtarefas salvas somem de uma vez.
5. 📌 "A chamada é real, feita pelo backend (`ServicoIA`), com a chave em variável de ambiente. Peço JSON estruturado com `responseSchema`."
6. *(Opcional, mostra robustez)* Parar o servidor, subir **sem** `GEMINI_API_KEY` e clicar ✨: aparece a mensagem explicando que a chave não está configurada.

## 7. Encerramento (0,5 min)
"Os cinco pilares estão implementados e integrados: pilha com push/pop/peek, SQLite com JPA, CRUD completo, POO com polimorfismo e uma integração real de IA. Limitações conhecidas: sem login e histórico de desfazer em memória. Fico à disposição para perguntas."

## Cola rápida: "onde está cada pilar?"

| Pilar | Arquivo para abrir |
|---|---|
| Estrutura de dados | `estrutura/PilhaDeAcoes.java` |
| Banco | `model/Tarefa.java`, `schema/script-banco.sql`, `smartkanban.db` |
| CRUD | `service/GerenciadorDeTarefas.java`, `controller/TarefaController.java` |
| POO | `estrutura/Acao.java` + subclasses |
| IA | `service/ServicoIA.java`, `controller/IAController.java` |

## Plano B (se algo falhar ao vivo)
- **IA sem resposta** (cota/internet): explique que o erro tratado é parte do requisito e mostre a mensagem.
- **Porta ocupada:** mude `server.port` ou feche o programa que a usa.
- **Banco travado:** feche ferramentas que abriram o `.db`.
