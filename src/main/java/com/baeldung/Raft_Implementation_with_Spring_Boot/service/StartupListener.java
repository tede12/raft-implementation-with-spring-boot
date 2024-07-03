package com.baeldung.Raft_Implementation_with_Spring_Boot.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final RaftService raftService;

    public StartupListener(RaftService raftService) {
        this.raftService = raftService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        raftService.initializeNode()
                .doOnError(e -> System.err.println("Error during node initialization: " + e.getMessage()))
                .subscribe();
    }
}
