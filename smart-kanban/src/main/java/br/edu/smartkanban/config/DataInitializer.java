package br.edu.smartkanban.config;

import br.edu.smartkanban.model.Quadro;
import br.edu.smartkanban.model.Usuario;
import br.edu.smartkanban.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Na primeira execução (banco vazio) cria um usuário e um quadro padrão.
 * Não há tela de login: o escopo do projeto são os 5 pilares, não autenticação.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner criarDadosIniciais(UsuarioRepository usuarios) {
        return args -> {
            if (usuarios.count() > 0) return;
            Usuario u = new Usuario("Aluno", "aluno@smartkanban.local", hash("senha123"));
            u.adicionarQuadro(new Quadro("Meu Quadro", "Quadro Kanban principal"));
            usuarios.save(u); // cascade grava o quadro junto
            System.out.println(">> Banco inicializado com usuário e quadro padrão.");
        };
    }

    /** Hash SHA-256 (suficiente para fins didáticos; em produção usar BCrypt). */
    private static String hash(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
