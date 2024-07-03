package com.baeldung.Raft_Implementation_with_Spring_Boot.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Setter
@Getter
@Table("node_state")
public class NodeStateEntity {
    @Id
    private Long id;

    @Column("node_id")
    private String nodeId;

    @Column("state")
    private String state;

    @Column("current_term")
    private int currentTerm;

    @Column("voted_for")
    private String votedFor;

    @Override
    public String toString() {
        return "NodeStateEntity{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", state='" + state + '\'' +
                ", currentTerm=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                '}';
    }
}
