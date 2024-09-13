package com.baeldung.Raft_Implementation_with_Spring_Boot.controller;

import com.baeldung.Raft_Implementation_with_Spring_Boot.dto.NodeStatusDTO;
import com.baeldung.Raft_Implementation_with_Spring_Boot.service.RaftService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Duration;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/raft")
@Tag(name = "Raft Operations", description = "Endpoints for Raft consensus operations")
public class RaftController {
    private final RaftService raftService;
    private final ObjectMapper objectMapper;

    public RaftController(RaftService raftService) {
        this.raftService = raftService;
        this.objectMapper = new ObjectMapper();
    }

    @Operation(summary = "Start a new election")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Election started successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/start-election")
    public Mono<Void> startElection() {
        return raftService.startElection();
    }

    @Operation(summary = "Request a vote from the node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vote granted or denied",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/request-vote")
    public Mono<Boolean> requestVote(
            @Parameter(description = "Vote request payload", required = true)
            @RequestBody Map<String, Object> payload) {
        // Recover the candidateId and candidateTerm from the payload
        String candidateId = (String) payload.get("candidateId");
        Integer candidateTerm = (payload.get("candidateTerm") instanceof Integer) ? (Integer) payload.get("candidateTerm") : null;

        // Verify that the candidateId and candidateTerm are not null
        if (candidateId == null || candidateTerm == null) {
            return Mono.error(new IllegalArgumentException("Invalid request payload: 'candidateId' or 'candidateTerm' is missing."));
        }
        return raftService.requestVote(candidateId, candidateTerm);
    }

    @Operation(summary = "Initialize the node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node initialized successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/initialize")
    public Mono<Void> initialize() {
        return raftService.initializeNode();
    }

    @Operation(summary = "Receive heartbeat from the leader")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Heartbeat received successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/heartbeat")
    public Mono<Void> receiveHeartbeat() {
        return raftService.receiveHeartbeat();
    }

    @Operation(summary = "Get the current node status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NodeStatusDTO.class))),
            @ApiResponse(responseCode = "404", description = "Node state not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/status")
    public Mono<NodeStatusDTO> getStatus() {
        return raftService.getNodeStatusEntity()
                .map(node -> new NodeStatusDTO(
                        node.getNodeId(),
                        node.getState(),
                        node.getCurrentTerm(),
                        node.getVotedFor(),
                        raftService.getOwnNodeUrl()
                ));
    }

    @Operation(summary = "Stream the status of all nodes in the cluster")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Streaming node statuses",
                    content = @Content(mediaType = "text/event-stream")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
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