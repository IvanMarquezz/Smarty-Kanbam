# E. Sabatina: perguntas e respostas

Respostas curtas e em linguagem simples. Procure *entender* cada uma e aponte o arquivo no código ao responder.

---

## Estrutura de dados (pilha)

**1. O que é uma pilha e como ela funciona?**
É uma estrutura em que só se insere e remove por uma ponta, o topo, como uma pilha de pratos. No projeto: `PilhaDeAcoes`, sobre um `ArrayDeque`.

**2. O que é LIFO?**
*Last In, First Out*: o último a entrar é o primeiro a sair. Por isso o Ctrl+Z desfaz primeiro a ação mais recente.

**3. Qual a diferença entre PUSH e POP?**
PUSH **coloca** um elemento no topo; POP **retira e devolve** o elemento do topo. Um adiciona, o outro remove.

**4. E o PEEK? Qual a diferença para o POP?**
PEEK só **consulta** o topo, sem remover. No projeto, uso o peek para mostrar "próxima ação a desfazer" e dentro de `desfazer()` para olhar a ação antes de executá-la.

**5. Qual a complexidade das operações da pilha?**
`push`, `pop` e `peek` são O(1) (tempo constante), pois só mexem na ponta do `ArrayDeque`.

**6. Por que `ArrayDeque` e não `Stack` ou `LinkedList`?**
A documentação do Java recomenda `Deque` para pilhas. `Stack` é uma classe antiga baseada em `Vector` (sincronizada sem necessidade). `ArrayDeque` é mais simples e eficiente que `LinkedList` por usar um vetor circular.

**7. O que acontece se eu der POP numa pilha vazia?**
Minha pilha lança `NoSuchElementException`. Mas o `GerenciadorDeTarefas` checa antes com `peek()` e devolve ao usuário a mensagem "Não há ações para desfazer".

**8. Para que serve a capacidade máxima da pilha?**
Evitar crescimento infinito de memória. Passando de 50 ações, descarto a mais antiga (a da base) com `removeLast()`.

**9. Como o Ctrl+Z funciona internamente, do teclado ao banco?**
O `app.js` captura `Ctrl+Z` → faz `POST /api/desfazer` → `DesfazerController` chama `gerenciador.desfazer()` → `peek` pega a ação do topo → `acao.desfazer()` reverte no banco → `pop` a remove → o frontend recarrega o quadro.

**10. Por que o Ctrl+Z não atrapalha quando estou digitando num campo?**
O listener ignora o atalho se o foco está em `input`, `textarea` ou `select`, preservando o "desfazer" nativo do texto.

**11. Por que as operações `reverter*` não empilham nada?**
Se empilhassem, desfazer criaria novas ações a desfazer, e o histórico nunca esvaziaria. Não implementei "refazer" (redo).

## POO

**12. Onde está o encapsulamento?**
Atributos `private` em todas as classes; `PilhaDeAcoes` esconde o `ArrayDeque` (só se vê push/pop/peek); `Tarefa.mudarStatus()` guarda a regra da data de conclusão e não há `setStatus` público; `Quadro.setUsuario` é package-private.

**13. Onde está o polimorfismo?**
Em `Acao.desfazer()`. `AcaoMoverTarefa`, `AcaoEditarTarefa`, `AcaoExcluirTarefa` e `AcaoCriarTarefa` implementam cada uma a sua reversão, e o gerenciador chama `topo.desfazer(this)` sem saber o tipo real.

**14. Onde há herança e por que ela faz sentido?**
As quatro ações herdam de `Acao` (classe abstrata): todas *são* ações com descrição e momento. Não criei `Subtarefa extends Tarefa` porque não teria nada de novo: seria herança artificial.

**15. O que é composição? Dê exemplos.**
Quando um objeto "possui" outro como parte de si: `Usuario` possui `Quadro`s, `Quadro` possui `Tarefa`s, `GerenciadorDeTarefas` possui a `PilhaDeAcoes`.

**16. O que é uma classe abstrata e por que `Acao` é abstrata?**
Uma classe que não pode ser instanciada e pode ter métodos sem corpo. `Acao` define *o que* toda ação deve fazer (`desfazer`), deixando o *como* para cada subclasse. Não existe "uma ação genérica".

**17. Para que serve a interface `OperacoesDeDesfazer`?**
As ações precisam chamar operações do gerenciador. Dependendo da interface, e não da classe concreta, reduzo o acoplamento e o código fica mais testável (no meu teste usei uma implementação falsa).

## Banco de dados e CRUD

**18. Qual a diferença entre CRUD e banco de dados?**
Banco de dados é **onde** os dados ficam guardados (SQLite). CRUD são as **operações** (Create, Read, Update, Delete) que a aplicação faz sobre eles. O CRUD acontece *no* banco.

**19. Como os dados são persistidos?**
Gravados pelo Hibernate/JPA no arquivo `smartkanban.db` (SQLite). Cada `save()` vira INSERT/UPDATE. Como estão em arquivo, sobrevivem ao fechar o sistema. Só a pilha de desfazer é em memória.

**20. O que é JPA/Hibernate e para que serve o Repository?**
JPA é a especificação de ORM (mapear objetos ↔ tabelas); Hibernate a implementa. `JpaRepository` me dá o CRUD pronto sem escrever SQL; o Spring cria a implementação em tempo de execução.

**21. Como foram modeladas as subtarefas?**
Auto-relacionamento: a tabela `tarefa` tem a coluna `tarefa_pai_id` apontando para `tarefa.id`. Subtarefa é uma tarefa com pai.

**22. O que acontece ao excluir uma tarefa que tem subtarefas? E ao desfazer?**
O `cascade = ALL` apaga as subtarefas junto. Antes de apagar, tiro *snapshots* de todas; ao desfazer, reinsiro tudo com os **mesmos ids**.

**23. Para que serve `@Transactional`?**
Faz o método rodar numa transação: ou todas as alterações são gravadas ou nenhuma (rollback em caso de erro), mantendo o banco consistente.

**24. Por que o pool de conexões é 1?**
O SQLite aceita um escritor por vez; com várias conexões poderia dar "database is locked".

## Arquitetura e IA

**25. Como funciona a comunicação entre frontend e backend?**
O JavaScript usa `fetch()` para chamar a API REST (HTTP + JSON), por exemplo `PATCH /api/tarefas/7/status`. O backend processa, grava no banco e devolve JSON; o JS redesenha o quadro. Eles são servidos pelo mesmo servidor (porta 8080).

**26. Para que servem os DTOs?**
Definem exatamente o que entra e sai da API e evitam serializar entidades JPA (que têm referências circulares pai ⇄ filho).

**27. Como funciona a integração com a IA?**
O backend (`ServicoIA`) envia título e descrição por HTTPS à API do Gemini, pedindo JSON estruturado (`responseSchema`). Converte a resposta em subtarefas com estimativa e devolve ao navegador. O usuário aceita ou descarta; só as aceitas são gravadas.

**28. Como a chave da API é protegida?**
Fica na variável de ambiente `GEMINI_API_KEY`, lida só pelo backend. O frontend nunca a recebe; o `.env` está no `.gitignore`.

**29. E se a IA falhar ou não houver chave?**
O `ServicoIA` trata ausência de chave, timeout, falta de internet, 400/403/404/429/5xx e JSON inválido, devolvendo uma mensagem clara que aparece no modal. Nunca finge que funcionou.

**30. Por que a pilha fica em memória e não no banco?**
Um "desfazer" é um recurso de sessão; guardar em memória é mais simples e rápido. Os dados importantes (tarefas) estão no banco. Persistir a pilha exigiria uma tabela de ações serializadas.

**31. Que limitações o projeto tem?**
Sem login (usuário/quadro padrão), uma pilha global, sem "refazer", e o histórico some ao reiniciar. São escolhas conscientes de escopo.
