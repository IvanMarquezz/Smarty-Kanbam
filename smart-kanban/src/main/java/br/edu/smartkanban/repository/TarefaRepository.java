package br.edu.smartkanban.repository;

import br.edu.smartkanban.model.Tarefa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    /** SELECT * FROM tarefa WHERE quadro_id = ? ORDER BY id */
    List<Tarefa> findByQuadroIdOrderByIdAsc(Long quadroId);
}
