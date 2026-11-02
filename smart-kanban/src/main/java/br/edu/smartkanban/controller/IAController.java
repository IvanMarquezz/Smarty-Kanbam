package br.edu.smartkanban.controller;

import br.edu.smartkanban.dto.AceitarSubtarefasRequest;
import br.edu.smartkanban.dto.PlanejarRequest;
import br.edu.smartkanban.dto.SubtarefaSugeridaDTO;
import br.edu.smartkanban.dto.TarefaResponse;
import br.edu.smartkanban.service.GerenciadorDeTarefas;
import br.edu.smartkanban.service.ServicoIA;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Endpoints do Auto-Planejamento com IA. */
@RestController
@RequestMapping("/api/ia")
public class IAController {

    private final ServicoIA servicoIA;
    private final GerenciadorDeTarefas gerenciador;

    public IAController(ServicoIA servicoIA, GerenciadorDeTarefas gerenciador) {
        this.servicoIA = servicoIA;
        this.gerenciador = gerenciador;
    }

    /** Informa se a chave está configurada (nunca devolve a chave). */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("configurada", servicoIA.estaConfigurada(), "modelo", servicoIA.getModelo());
    }

    /** Pede sugestões à IA. NÃO grava nada: o usuário ainda vai aceitar ou descartar. */
    @PostMapping("/planejar")
    public List<SubtarefaSugeridaDTO> planejar(@RequestBody PlanejarRequest request) {
        return servicoIA.planejar(request.titulo(), request.descricao());
    }

    /** Grava como tarefas individuais (vinculadas à principal) apenas as sugestões aceitas. */
    @PostMapping("/aceitar")
    public List<TarefaResponse> aceitar(@RequestBody AceitarSubtarefasRequest request) {
        return gerenciador.criarSubtarefas(request.tarefaPaiId(), request.subtarefas());
    }
}
