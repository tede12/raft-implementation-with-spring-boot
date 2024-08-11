package com.baeldung.Raft_Implementation_with_Spring_Boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "node")
public class NodeConfig {
    private String id;
    private List<String> clusterNodes;
}
