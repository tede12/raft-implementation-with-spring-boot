package com.baeldung.Raft_Implementation_with_Spring_Boot.service;

import com.baeldung.Raft_Implementation_with_Spring_Boot.model.NodeStateEntity;
import com.baeldung.Raft_Implementation_with_Spring_Boot.repository.NodeStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TransactionalRaftService {
    private final NodeStateRepository nodeStateRepository;

    public TransactionalRaftService(NodeStateRepository nodeStateRepository) {
        this.nodeStateRepository = nodeStateRepository;
    }

    @Transactional
    public Mono<NodeStateEntity> saveNodeState(NodeStateEntity node) {
        return nodeStateRepository.save(node)
                .doOnSuccess(savedNode -> log.info("Node state saved: {}", savedNode))
                .doOnError(e -> log.error("Error saving node state: {}", e.getMessage()));
    }

    @Transactional
    public Mono<Void> stepDown(NodeStateEntity node) {
        node.setState("FOLLOWER");
        return nodeStateRepository.save(node)
                .doOnSuccess(savedNode -> log.info("Node {} has stepped down to FOLLOWER", savedNode.getNodeId()))
                .then();
    }

    @Transactional
    public Mono<NodeStateEntity> becomeLeader(NodeStateEntity node) {
        node.setState("LEADER");
        return nodeStateRepository.save(node)
                .doOnSuccess(savedNode -> log.info("Node {} has become LEADER with term {}", savedNode.getNodeId(), savedNode.getCurrentTerm()))
                .thenReturn(node);
    }
}
