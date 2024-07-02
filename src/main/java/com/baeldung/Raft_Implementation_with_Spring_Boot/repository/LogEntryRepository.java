package com.baeldung.Raft_Implementation_with_Spring_Boot.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import com.baeldung.Raft_Implementation_with_Spring_Boot.model.LogEntryEntity;

public interface LogEntryRepository extends ReactiveCrudRepository<LogEntryEntity, Long> {
}