package com.baeldung.Raft_Implementation_with_Spring_Boot;

import com.baeldung.Raft_Implementation_with_Spring_Boot.repository.NodeStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
class RaftImplementationWithSpringBootApplicationTests {

	@Test
	void contextLoads() {
	}

}

@SpringBootTest
class NodeStateRepositoryTests {

    @Autowired
    private NodeStateRepository nodeStateRepository;

    @Test
    void testRepositoryInjection() {
        StepVerifier.create(nodeStateRepository.findAll())
                    .expectNextCount(0)
                    .verifyComplete();
    }
}
