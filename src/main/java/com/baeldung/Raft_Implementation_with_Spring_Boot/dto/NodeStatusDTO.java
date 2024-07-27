package com.baeldung.Raft_Implementation_with_Spring_Boot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NodeStatusDTO {
    private String nodeId;
    private String state;
    private int currentTerm;
    private String votedFor;
    private String nodeUrl;

    // Constructor for Nodes that are not available and do not have a nodeUrl
    public NodeStatusDTO(String nodeId, String state, int currentTerm, String votedFor) {
        this.nodeId = nodeId;
        this.state = state;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.nodeUrl = null;
    }

    // Constructor for Nodes that are available and have a nodeUrl
    public NodeStatusDTO(String nodeId, String state, int currentTerm, String votedFor, String nodeUrl) {
        this.nodeId = nodeId;
        this.state = state;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.nodeUrl = nodeUrl;
    }
}
