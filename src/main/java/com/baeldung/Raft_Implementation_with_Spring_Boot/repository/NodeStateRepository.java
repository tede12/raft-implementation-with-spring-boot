package com.baeldung.Raft_Implementation_with_Spring_Boot.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import com.baeldung.Raft_Implementation_with_Spring_Boot.model.NodeStateEntity;

public interface NodeStateRepository extends ReactiveCrudRepository<NodeStateEntity, Long> {
    Mono<NodeStateEntity> findByNodeId(String nodeId);

}


