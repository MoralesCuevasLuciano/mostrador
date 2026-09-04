package com.pepology.mostrador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API Spring Boot del mostrador.
 * Arranca Tomcat embebido, Flyway y el resto de beans del catálogo.
 */
@SpringBootApplication
public class MostradorApplication {

	/** Levanta la aplicación con el perfil activo (local en desarrollo). */
	public static void main(String[] args) {
		SpringApplication.run(MostradorApplication.class, args);
	}

}
