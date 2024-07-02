package com.baeldung.Raft_Implementation_with_Spring_Boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "com.baeldung.Raft_Implementation_with_Spring_Boot.repository")
public class RaftImplementationWithSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaftImplementationWithSpringBootApplication.class, args);
    }
}
