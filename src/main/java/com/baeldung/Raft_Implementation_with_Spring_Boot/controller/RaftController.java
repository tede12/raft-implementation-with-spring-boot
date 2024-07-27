package com.baeldung.Raft_Implementation_with_Spring_Boot.controller;

import com.baeldung.Raft_Implementation_with_Spring_Boot.dto.NodeStatusDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.baeldung.Raft_Implementation_with_Spring_Boot.service.RaftService;

import java.time.Duration;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/raft")
public class RaftController {
    private final RaftService raftService;
    private final ObjectMapper objectMapper;

    public RaftController(RaftService raftService) {
        this.raftService = raftService;
        this.objectMapper = new ObjectMapper();
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


    @PostMapping("/initialize")
    public Mono<Void> initialize() {
        return raftService.initializeNode();
    }


    @PostMapping("/heartbeat")
    public Mono<Void> receiveHeartbeat() {
        return raftService.receiveHeartbeat();
    }

    @GetMapping("/status")
    public Mono<NodeStatusDTO> getStatus() {
        return raftService.getNodeStatusEntity().map(node -> new NodeStatusDTO(node.getNodeId(), node.getState(), node.getCurrentTerm(), node.getVotedFor(), raftService.getOwnNodeUrl()));
    }

    @GetMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamStatus() {
        return Flux.interval(Duration.ofSeconds(2)).flatMap(tick -> raftService.getAllNodeStatuses()).map(nodeStates -> {
            // Convert the list of NodeStatusDTO objects to a JSON string
            try {
                return objectMapper.writeValueAsString(nodeStates);
            } catch (JsonProcessingException e) {
                        log.error("Error serializing node states: {}", e.getMessage());
                return "[]";
            }
        });
    }

}