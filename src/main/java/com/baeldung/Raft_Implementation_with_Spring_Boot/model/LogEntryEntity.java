package com.baeldung.Raft_Implementation_with_Spring_Boot.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("log_entry")
public class LogEntryEntity {
    // Getters and Setters
    @Id
    private Long id;
    private int term;
    private String command;

}