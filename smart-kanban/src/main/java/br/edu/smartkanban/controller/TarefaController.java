package br.edu.smartkanban.controller;

import br.edu.smartkanban.dto.StatusRequest;
import br.edu.smartkanban.dto.TarefaRequest;
import br.edu.smartkanban.dto.TarefaResponse;
import br.edu.smartkanban.service.GerenciadorDeTarefas;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Camada REST do CRUD: recebe HTTP/JSON e delega ao GerenciadorDeTarefas (sem regra de negócio aqui). */
@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final GerenciadorDeTarefas gerenciador;

    public TarefaController(GerenciadorDeTarefas gerenciador) { this.gerenciador = gerenciador; }

    @GetMapping
    public List<TarefaResponse> listar() { return gerenciador.listar(); }

    @GetMapping("/{id}")
    public TarefaResponse buscar(@PathVariable Long id) { return gerenciador.buscar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaResponse criar(@RequestBody TarefaRequest request) { return gerenciador.criar(request); }

    @PutMapping("/{id}")
    public TarefaResponse editar(@PathVariable Long id, @RequestBody TarefaRequest request) {
        return gerenciador.editar(id, request);
    }

    @PatchMapping("/{id}/status")
    public TarefaResponse mudarStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return gerenciador.mudarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { gerenciador.excluir(id); }
}
