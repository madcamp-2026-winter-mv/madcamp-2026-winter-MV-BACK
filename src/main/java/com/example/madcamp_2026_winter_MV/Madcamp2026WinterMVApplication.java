package com.example.madcamp_2026_winter_MV; // 여기도 MV로 확인!

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class Madcamp2026WinterMVApplication {

	public static void main(String[] args) {
		SpringApplication.run(Madcamp2026WinterMVApplication.class, args);
	}

}