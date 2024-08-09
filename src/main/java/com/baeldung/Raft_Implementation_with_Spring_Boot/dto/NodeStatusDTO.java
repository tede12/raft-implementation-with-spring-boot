package com.baeldung.Raft_Implementation_with_Spring_Boot.dto;

import com.baeldung.Raft_Implementation_with_Spring_Boot.model.NodeState;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NodeStatusDTO {
    private String nodeId;
    private NodeState state;
    private int currentTerm;
    private String votedFor;
    private String nodeUrl;

    public NodeStatusDTO(String nodeId, NodeState state, int currentTerm, String votedFor, String nodeUrl) {
        this.nodeId = nodeId;
        this.state = state;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.nodeUrl = nodeUrl;
    }
}
