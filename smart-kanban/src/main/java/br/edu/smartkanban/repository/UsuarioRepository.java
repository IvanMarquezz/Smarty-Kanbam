package br.edu.smartkanban.repository;

import br.edu.smartkanban.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

/** O Spring Data gera a implementação (INSERT/SELECT/UPDATE/DELETE) automaticamente. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
