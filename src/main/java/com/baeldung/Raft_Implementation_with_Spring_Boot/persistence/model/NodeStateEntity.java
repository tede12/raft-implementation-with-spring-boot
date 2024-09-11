package com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;


@Setter
@Getter
@Table("node_state")
@EqualsAndHashCode
public class NodeStateEntity {
    @Id
    @Schema(description = "Unique identifier of the record", example = "1")
    private Long id;

    @Column("node_id")
    @Schema(description = "Unique identifier of the node", example = "node1")
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column("state")
    @Schema(description = "Current state of the node", example = "FOLLOWER")
    private NodeState state;

    @Column("current_term")
    @Schema(description = "Current term number", example = "1")
    private int currentTerm;

    @Column("voted_for")
    @Schema(description = "Node ID that this node has voted for", example = "node2")
    private String votedFor;

    @Override
    public String toString() {
        return "NodeStateEntity{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", state=" + state +
                ", currentTerm=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                '}';
    }
}
