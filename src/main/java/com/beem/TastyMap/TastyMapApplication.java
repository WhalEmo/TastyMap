package com.beem.TastyMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(
		exclude = {
				org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration.class
		}
)
public class TastyMapApplication {

	public static void main(String[] args) {
		SpringApplication.run(TastyMapApplication.class, args);
	}

}
