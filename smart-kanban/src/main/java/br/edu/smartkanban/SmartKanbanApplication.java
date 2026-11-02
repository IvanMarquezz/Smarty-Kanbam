package br.edu.smartkanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada. O Spring Boot sobe o servidor web, o JPA e serve o frontend (static/). */
@SpringBootApplication
public class SmartKanbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartKanbanApplication.class, args);
    }
}
