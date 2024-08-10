package com.baeldung.Raft_Implementation_with_Spring_Boot.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final RaftService raftService;

    public StartupListener(RaftService raftService) {
        this.raftService = raftService;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("\n\n");
        log.info("****************************************************************************************");
        log.info("Node is starting up with the following configuration:");
        log.info("\tNode ID: {}", raftService.getNodeId());
        log.info("\tNode URL: {}", raftService.getOwnNodeUrl());
        log.info("\tCluster Nodes: {}", String.join(", ", raftService.getClusterNodes()));
        log.debug("\tDEBUGGING MONITOR: http://{}/monitor", raftService.getOwnNodeUrl());
        log.info("****************************************************************************************\n\n");

        // Initialize the node
        raftService.initializeNode()
                .doOnError(e -> log.error("Error during node initialization: {}", e.getMessage()))
                .subscribe();
    }
}
