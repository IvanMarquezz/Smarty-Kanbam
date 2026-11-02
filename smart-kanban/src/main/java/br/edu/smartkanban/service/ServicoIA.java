package br.edu.smartkanban.service;

import br.edu.smartkanban.dto.SubtarefaSugeridaDTO;
import br.edu.smartkanban.excecao.ServicoIAException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Integração REAL com a API do Google Gemini (REST), feita pelo BACKEND.
 * A chave vem da variável de ambiente GEMINI_API_KEY e nunca chega ao navegador.
 * Usa apenas java.net.http.HttpClient (já faz parte do Java) e o Jackson (já vem no Spring).
 */
@Service
public class ServicoIA {

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_SUBTAREFAS = 10;

    private final String apiKey;
    private final String modelo;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ServicoIA(@Value("${gemini.api-key:}") String apiKey,
                     @Value("${gemini.model}") String modelo,
                     @Value("${gemini.timeout-seconds:30}") long timeoutSegundos) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean estaConfigurada() { return !apiKey.isBlank(); }

    public String getModelo() { return modelo; }

    /** Envia título + descrição ao Gemini e devolve as subtarefas sugeridas (com estimativa). */
    public List<SubtarefaSugeridaDTO> planejar(String titulo, String descricao) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Informe o título da tarefa para planejar com IA.");
        }
        if (!estaConfigurada()) {
            throw new ServicoIAException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave da API de IA não está configurada. Defina a variável de ambiente "
                    + "GEMINI_API_KEY e reinicie o sistema (veja o README). Nenhuma sugestão foi gerada.");
        }

        String corpo = montarCorpo(titulo, descricao);
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(URL_BASE + modelo + ":generateContent"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)   // chave enviada só do servidor para o Google
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resposta;
        try {
            resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ServicoIAException(HttpStatus.GATEWAY_TIMEOUT,
                    "A IA demorou demais para responder. Tente novamente em instantes.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoIAException(HttpStatus.SERVICE_UNAVAILABLE, "A requisição à IA foi interrompida.");
        } catch (IOException e) {
            throw new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível conectar à API de IA. Verifique sua conexão com a internet.");
        }

        if (resposta.statusCode() != 200) {
            throw traduzirErroHttp(resposta.statusCode(), resposta.body());
        }
        return interpretarResposta(resposta.body());
    }

    /** Monta o JSON da requisição. Pedimos resposta ESTRUTURADA (array de objetos) via responseSchema. */
    private String montarCorpo(String titulo, String descricao) {
        String prompt = "Você é um assistente de planejamento de projetos. Divida a tarefa abaixo em "
                + "3 a 8 subtarefas pequenas, concretas e em ordem lógica de execução. Para cada subtarefa "
                + "informe um título curto, uma descrição de uma frase e uma estimativa de tempo realista "
                + "(exemplos: \"30 minutos\", \"2 horas\", \"1 dia\"). Responda em português do Brasil.\n\n"
                + "TÍTULO: " + titulo.trim() + "\n"
                + "DESCRIÇÃO: " + (descricao == null || descricao.isBlank() ? "(sem descrição)" : descricao.trim());

        ObjectNode raiz = mapper.createObjectNode();
        raiz.putArray("contents").addObject().putArray("parts").addObject().put("text", prompt);

        ObjectNode config = raiz.putObject("generationConfig");
        config.put("responseMimeType", "application/json");
        config.put("temperature", 0.4);

        ObjectNode schema = config.putObject("responseSchema");
        schema.put("type", "ARRAY");
        ObjectNode item = schema.putObject("items");
        item.put("type", "OBJECT");
        ObjectNode props = item.putObject("properties");
        props.putObject("titulo").put("type", "STRING");
        props.putObject("descricao").put("type", "STRING");
        props.putObject("estimativa").put("type", "STRING");
        item.putArray("required").add("titulo").add("estimativa");

        return raiz.toString();
    }

    /** Converte códigos HTTP de erro em mensagens compreensíveis. */
    private ServicoIAException traduzirErroHttp(int codigo, String corpo) {
        String detalhe = extrairMensagemDeErro(corpo);
        return switch (codigo) {
            case 400 -> new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    (corpo != null && corpo.contains("API_KEY_INVALID"))
                            ? "A chave da API de IA é inválida. Confira o valor de GEMINI_API_KEY."
                            : "A API de IA recusou a requisição (400): " + detalhe);
            case 401, 403 -> new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    "A chave da API de IA não tem permissão de acesso (" + codigo + "): " + detalhe);
            case 404 -> new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    "O modelo '" + modelo + "' não foi encontrado. Ajuste GEMINI_MODEL com um modelo "
                    + "disponível (consulte a documentação do Gemini).");
            case 429 -> new ServicoIAException(HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de uso gratuito da IA atingido. Aguarde um pouco e tente novamente.");
            default -> new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    codigo >= 500 ? "A API de IA está indisponível no momento (" + codigo + "). Tente mais tarde."
                                  : "Erro inesperado da API de IA (" + codigo + "): " + detalhe);
        };
    }

    private String extrairMensagemDeErro(String corpo) {
        try {
            String msg = mapper.readTree(corpo).path("error").path("message").asText("");
            return msg.isBlank() ? "sem detalhes" : msg;
        } catch (Exception e) {
            return "sem detalhes";
        }
    }

    /** Lê candidates[0].content.parts[*].text e converte o JSON de texto em objetos Java. */
    private List<SubtarefaSugeridaDTO> interpretarResposta(String corpoHttp) {
        String texto;
        try {
            StringBuilder sb = new StringBuilder();
            for (JsonNode parte : mapper.readTree(corpoHttp).path("candidates").path(0).path("content").path("parts")) {
                if (!parte.path("thought").asBoolean(false)) sb.append(parte.path("text").asText(""));
            }
            texto = sb.toString().trim();
        } catch (IOException e) {
            throw new ServicoIAException(HttpStatus.BAD_GATEWAY, "A resposta da IA veio em formato ilegível.");
        }
        if (texto.isEmpty()) {
            throw new ServicoIAException(HttpStatus.BAD_GATEWAY,
                    "A IA não devolveu nenhum conteúdo (a resposta pode ter sido bloqueada). Reescreva a descrição e tente de novo.");
        }
        // Defesa extra: remove cercas de markdown (```json ... ```) caso o modelo as envie
        texto = texto.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("(?s)\\s*```$", "").trim();

        try {
            JsonNode json = mapper.readTree(texto);
            JsonNode lista = json.isArray() ? json : json.path("subtarefas");
            if (!(lista instanceof ArrayNode)) throw new IOException("não é uma lista");

            List<SubtarefaSugeridaDTO> resultado = new ArrayList<>();
            for (JsonNode no : lista) {
                String titulo = no.path("titulo").asText("").trim();
                if (titulo.isEmpty() || resultado.size() >= MAX_SUBTAREFAS) continue;
                resultado.add(new SubtarefaSugeridaDTO(titulo,
                        no.path("descricao").asText("").trim(),
                        no.path("estimativa").asText("").trim()));
            }
            if (resultado.isEmpty()) {
                throw new ServicoIAException(HttpStatus.BAD_GATEWAY, "A IA não sugeriu nenhuma subtarefa. Tente detalhar mais a descrição.");
            }
            return resultado;
        } catch (IOException e) {
            throw new ServicoIAException(HttpStatus.BAD_GATEWAY, "A IA devolveu uma resposta fora do formato esperado. Tente novamente.");
        }
    }
}
