package com.baeldung.Raft_Implementation_with_Spring_Boot.model;

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
    private Long id;

    @Column("node_id")
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column("state")
    private NodeState state;

    @Column("current_term")
    private int currentTerm;

    @Column("voted_for")
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
