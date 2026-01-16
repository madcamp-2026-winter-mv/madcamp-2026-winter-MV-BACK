package com.example.madcamp_2026_winter_MV;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Madcamp2026WinterMvApplication {

	public static void main(String[] args) {
		SpringApplication.run(Madcamp2026WinterMvApplication.class, args);
	}

}