package com.baeldung.Raft_Implementation_with_Spring_Boot.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import com.baeldung.Raft_Implementation_with_Spring_Boot.service.RaftService;

import java.util.Map;

@RestController
@RequestMapping("/raft")
public class RaftController {
    private final RaftService raftService;

    public RaftController(RaftService raftService) {
        this.raftService = raftService;
    }

    @PostMapping("/request-vote")
    public Mono<Boolean> requestVote(@RequestBody Map<String, Object> payload) {
        // Get the candidateId and candidateTerm from the payload
        String candidateId = (String) payload.get("candidateId");
        Integer candidateTerm = (payload.get("candidateTerm") instanceof Integer) ? (Integer) payload.get("candidateTerm") : null;

        // Check if the candidateId and candidateTerm are present in the payload
        if (candidateId == null || candidateTerm == null) {
            return Mono.error(new IllegalArgumentException("Invalid request payload: 'candidateId' or 'candidateTerm' is missing."));
        }

        return raftService.requestVote(candidateId, candidateTerm);
    }

    @PostMapping("/start-election")
    public Mono<Void> startElection() {
        return raftService.startElection();
    }

    @GetMapping("/status")
    public Mono<String> getStatus() {
        return raftService.getNodeStatus();
    }

    @PostMapping("/initialize")
    public Mono<Void> initialize() {
        return raftService.initializeNode();
    }


    @PostMapping("/heartbeat")
    public Mono<Void> receiveHeartbeat() {
        return raftService.receiveHeartbeat();
    }
}