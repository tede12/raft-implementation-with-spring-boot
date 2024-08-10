package com.baeldung.Raft_Implementation_with_Spring_Boot;

import com.baeldung.Raft_Implementation_with_Spring_Boot.config.NodeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NodeConfig.class)
public class RaftImplementationWithSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaftImplementationWithSpringBootApplication.class, args);
    }
}
