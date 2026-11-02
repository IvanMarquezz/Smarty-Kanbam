# A. Manual de instalação

## 1. Pré-requisitos

| Item | Versão | Como conferir |
|---|---|---|
| JDK (Java) | **21** | `java -version` deve mostrar 21 |
| Maven | 3.9 ou superior | `mvn -version` |
| Navegador | Chrome, Edge, Firefox (atuais) | — |
| Internet | Só na 1ª execução (Maven baixa as dependências) e para usar a IA | — |

Instalação sugerida: JDK 21 (Temurin/Adoptium ou Oracle) e Maven (<https://maven.apache.org/download.cgi>). Garanta que `JAVA_HOME` aponta para o JDK 21 e que `mvn` está no `PATH`.

Se você usa IntelliJ ou VS Code, pode importar a pasta como projeto Maven e rodar `SmartKanbanApplication`.

## 2. Dependências

Não instale nada à mão: o Maven lê o `pom.xml` e baixa tudo sozinho. As dependências são só quatro:
`spring-boot-starter-web` (servidor + REST + JSON), `spring-boot-starter-data-jpa` (Hibernate/JPA), `sqlite-jdbc` (driver) e `hibernate-community-dialects` (ensina o Hibernate a falar SQL do SQLite).

## 3. Configurar a chave da IA (Google Gemini)

1. Acesse <https://aistudio.google.com/apikey>, entre com sua conta Google e crie uma chave gratuita.
2. Copie `.env.example` para `.env` e troque `cole_sua_chave_aqui` pela chave.
3. Nunca publique o `.env` (o `.gitignore` já o ignora). A chave só é lida pelo **backend**; o navegador nunca a recebe.

Alternativa sem `.env` (variável só no terminal atual):

```bash
export GEMINI_API_KEY="sua_chave"            # Linux/Mac
$env:GEMINI_API_KEY="sua_chave"              # Windows PowerShell
set GEMINI_API_KEY=sua_chave                 # Windows CMD
mvn spring-boot:run
```

O modelo padrão é `gemini-3.1-flash-lite` (tem uso gratuito). Modelos mudam com o tempo: se aparecer "modelo não encontrado", escolha outro em <https://ai.google.dev/gemini-api/docs/models> e defina `GEMINI_MODEL` no `.env`.

## 4. Executar

```bash
./executar.sh        # Linux/Mac     (Windows: executar.bat)
# ou, diretamente:  mvn spring-boot:run
```

Aguarde a linha `Started SmartKanbanApplication`. Para parar: `Ctrl + C` no terminal.

Gerar um `.jar`: `mvn clean package` e depois `java -jar target/smart-kanban-1.0.0.jar`.

## 5. Acessar

Abra <http://localhost:8080>. (Se a porta 8080 estiver ocupada, mude `server.port` em `application.properties`.)

## 6. Verificar se o banco está funcionando

1. Após a primeira execução aparece o arquivo **`smartkanban.db`** na pasta do projeto.
2. Crie uma tarefa no navegador, **pare o servidor, rode de novo** e confirme que a tarefa continua lá. Isso prova a persistência.
3. Pela API: <http://localhost:8080/api/tarefas> deve mostrar o JSON das tarefas.
4. Direto no banco (se tiver o `sqlite3` instalado, ou use o "DB Browser for SQLite"):

```bash
sqlite3 smartkanban.db ".tables"
sqlite3 smartkanban.db "SELECT id, titulo, status, tarefa_pai_id FROM tarefa;"
```

## 7. Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `release version 21 not supported` | Maven usando Java antigo. Ajuste `JAVA_HOME` para o JDK 21. |
| `Port 8080 was already in use` | Feche o outro programa ou mude `server.port`. |
| `database is locked` | Há outro programa com o `smartkanban.db` aberto. Feche-o. |
| Botão ✨ diz que a chave não está configurada | `.env` ausente/errado, ou o servidor não foi reiniciado. |
| ✨ "chave inválida" / "modelo não encontrado" / "limite atingido" | Confira a chave, troque `GEMINI_MODEL`, ou espere a cota renovar. |
| Quer zerar tudo | Pare o servidor e apague `smartkanban.db`. |
