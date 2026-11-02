# 🧠 Smart Kanban

Gerenciador inteligente de tarefas (Kanban) com **desfazer (Ctrl+Z) baseado em Pilha** e **Auto-Planejamento com IA (Google Gemini)**.

- **Aluno:** Ivanildo Marques de Souza Filho · **Matrícula:** 1211539404
- **Disciplina:** Estrutura de Dados · **Professor:** Amaury Nogueira Neto

**Stack:** Java 21 · Spring Boot 3.3 · Maven · Spring Data JPA/Hibernate · SQLite · HTML/CSS/JS puros.

## Execução rápida

```bash
# 1) pré-requisitos: JDK 21 e Maven 3.9+ instalados (java -version / mvn -version)
# 2) (opcional, para a IA) copie .env.example para .env e coloque sua chave do Gemini
cp .env.example .env          # Windows: copy .env.example .env
# 3) rode
./executar.sh                 # Windows: executar.bat
# 4) abra http://localhost:8080
```

Sem o `.env` o sistema funciona normalmente; apenas o botão ✨ da IA avisa que a chave não está configurada.
Instruções completas (e como verificar o banco): [docs/01-manual-instalacao.md](docs/01-manual-instalacao.md).

## Estrutura

```
smart-kanban/
├── pom.xml  .env.example  executar.sh  executar.bat  README.md
├── docs/                      ← material de estudo e defesa (leia na ordem)
│   ├── 01-manual-instalacao.md
│   ├── 02-explicacao-tecnica-5-pilares.md
│   ├── 03-explicacao-das-classes.md
│   ├── 04-estrutura-de-dados-pilha.md
│   ├── 05-sabatina-perguntas-e-respostas.md
│   └── 06-roteiro-apresentacao.md
└── src/main/
    ├── java/br/edu/smartkanban/
    │   ├── SmartKanbanApplication.java
    │   ├── config/      DataInitializer          (cria usuário e quadro iniciais)
    │   ├── model/       Usuario, Quadro, Tarefa, Prioridade, StatusTarefa, DataConverters   (entidades)
    │   ├── repository/  UsuarioRepository, QuadroRepository, TarefaRepository               (acesso ao banco)
    │   ├── estrutura/   PilhaDeAcoes, Acao (+4 subclasses), TarefaSnapshot, OperacoesDeDesfazer
    │   ├── service/     GerenciadorDeTarefas (CRUD + pilha), ServicoIA (Gemini)
    │   ├── controller/  TarefaController, DesfazerController, IAController                  (API REST)
    │   ├── dto/         objetos de entrada/saída da API
    │   └── excecao/     exceções e tradução para respostas HTTP
    └── resources/
        ├── application.properties
        ├── schema/script-banco.sql    (modelo do banco, referência)
        └── static/    index.html, css/style.css, js/app.js
```

## Os 5 pilares (onde estão)

| Pilar | Onde |
|---|---|
| 1. Estruturas de dados | `estrutura/PilhaDeAcoes.java` (push/pop/peek sobre `ArrayDeque`) |
| 2. Banco de dados | `model/*` + `schema/script-banco.sql` + `smartkanban.db` (SQLite) |
| 3. Database Application (CRUD) | `repository/*`, `service/GerenciadorDeTarefas`, `controller/TarefaController` |
| 4. POO | `Acao` (abstrata) e subclasses; encapsulamento em `Tarefa`/`PilhaDeAcoes`; composição |
| 5. IA | `service/ServicoIA.java`, `controller/IAController.java` |

## API REST

| Método | Rota | Função |
|---|---|---|
| GET | `/api/tarefas` · `/api/tarefas/{id}` | listar · detalhes |
| POST | `/api/tarefas` | criar |
| PUT | `/api/tarefas/{id}` | editar |
| PATCH | `/api/tarefas/{id}/status` | mover de coluna |
| DELETE | `/api/tarefas/{id}` | excluir |
| POST | `/api/desfazer` | desfazer (pop) |
| GET | `/api/desfazer/estado` | topo da pilha (peek) e tamanho |
| GET | `/api/ia/status` | chave configurada? (nunca devolve a chave) |
| POST | `/api/ia/planejar` | pede subtarefas à IA (não grava) |
| POST | `/api/ia/aceitar` | grava as subtarefas aceitas |

## Limitações conhecidas (e por quê)

- **Sem login:** existe a entidade `Usuario`, mas há um usuário/quadro padrão. Autenticação não faz parte dos 5 pilares.
- **Pilha em memória:** o histórico do Ctrl+Z zera ao reiniciar o servidor (as tarefas permanecem no banco). É o comportamento esperado de um "desfazer" de sessão.
