package com.baeldung.Raft_Implementation_with_Spring_Boot.repository;

import com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.model.NodeState;
import com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.model.NodeStateEntity;
import com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.repository.NodeStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
class NodeStateRepositoryTest {

    @Autowired
    private NodeStateRepository nodeStateRepository;

    @Test
    void testFindByNodeId_Found() {
        NodeStateEntity node = new NodeStateEntity();
        node.setNodeId("node1");
        node.setState(NodeState.FOLLOWER);
        node.setCurrentTerm(1);

        Mono<NodeStateEntity> saveMono = nodeStateRepository.save(node);

        StepVerifier.create(saveMono)
                .expectNextMatches(savedNode -> savedNode.getNodeId().equals("node1") && savedNode.getState() == NodeState.FOLLOWER)
                .verifyComplete();

        Mono<NodeStateEntity> findMono = nodeStateRepository.findByNodeId("node1");

        StepVerifier.create(findMono)
                .expectNextMatches(foundNode -> foundNode.getNodeId().equals("node1") && foundNode.getState() == NodeState.FOLLOWER)
                .verifyComplete();
    }

    @Test
    void testFindByNodeId_NotFound() {
        Mono<NodeStateEntity> findMono = nodeStateRepository.findByNodeId("nonexistent");

        StepVerifier.create(findMono)
                .verifyComplete();
    }
}
