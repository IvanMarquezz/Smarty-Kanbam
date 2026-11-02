package br.edu.smartkanban.controller;

import br.edu.smartkanban.dto.EstadoDesfazerResponse;
import br.edu.smartkanban.service.GerenciadorDeTarefas;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Endpoints do CTRL+Z: POST desfaz (pop), GET informa o topo da pilha (peek). */
@RestController
@RequestMapping("/api/desfazer")
public class DesfazerController {

    private final GerenciadorDeTarefas gerenciador;

    public DesfazerController(GerenciadorDeTarefas gerenciador) { this.gerenciador = gerenciador; }

    @PostMapping
    public Map<String, String> desfazer() {
        return Map.of("mensagem", "Desfeito: " + gerenciador.desfazer());
    }

    @GetMapping("/estado")
    public EstadoDesfazerResponse estado() { return gerenciador.estadoDesfazer(); }
}
