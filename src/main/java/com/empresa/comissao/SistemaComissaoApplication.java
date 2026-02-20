package com.empresa.comissao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SistemaComissaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaComissaoApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.TimeZone tz = java.util.TimeZone.getDefault();
		org.slf4j.LoggerFactory.getLogger(SistemaComissaoApplication.class)
				.info("🌍 JVM TimeZone: {} ({}) | Offset: {}",
						tz.getID(),
						tz.getDisplayName(),
						java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()));
	}
}
