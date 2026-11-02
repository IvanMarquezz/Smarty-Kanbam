package br.edu.smartkanban.repository;

import br.edu.smartkanban.model.Quadro;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuadroRepository extends JpaRepository<Quadro, Long> {
    /** Consulta derivada do nome do método: SELECT ... ORDER BY id LIMIT 1 */
    Optional<Quadro> findFirstByOrderByIdAsc();
}
