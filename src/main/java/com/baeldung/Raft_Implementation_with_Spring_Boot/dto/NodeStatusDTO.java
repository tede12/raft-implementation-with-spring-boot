package com.baeldung.Raft_Implementation_with_Spring_Boot.dto;

import com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.model.NodeState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NodeStatusDTO {
    @Schema(description = "Unique identifier of the node", example = "node1")
    private String nodeId;

    @Schema(description = "Current state of the node in the Raft cluster", example = "FOLLOWER")
    private NodeState state;

    @Schema(description = "Current term number of the node", example = "1")
    private int currentTerm;

    @Schema(description = "ID of the node this node has voted for in the current term", example = "node2")
    private String votedFor;

    @Schema(description = "URL of the node", example = "localhost:8000")
    private String nodeUrl;

    public NodeStatusDTO(String nodeId, NodeState state, int currentTerm, String votedFor, String nodeUrl) {
        this.nodeId = nodeId;
        this.state = state;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.nodeUrl = nodeUrl;
    }
}
