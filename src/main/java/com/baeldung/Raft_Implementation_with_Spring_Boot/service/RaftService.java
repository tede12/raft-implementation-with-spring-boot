package com.baeldung.Raft_Implementation_with_Spring_Boot.service;

import com.baeldung.Raft_Implementation_with_Spring_Boot.dto.NodeStatusDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.baeldung.Raft_Implementation_with_Spring_Boot.model.NodeStateEntity;
import com.baeldung.Raft_Implementation_with_Spring_Boot.repository.NodeStateRepository;
import com.baeldung.Raft_Implementation_with_Spring_Boot.exception.NodeStateNotFoundException;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class RaftService {
    private final NodeStateRepository nodeStateRepository;
    private final TransactionalRaftService transactionalRaftService;
    private final WebClient webClient;

    private final String nodeId;
    private final List<String> clusterNodes;
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    @Getter
    private final String ownNodeUrl;
    private volatile long lastHeartbeat = System.currentTimeMillis();

    public RaftService(NodeStateRepository nodeStateRepository,
                       TransactionalRaftService transactionalRaftService,
                       @Value("${node.id}") String nodeId,
                       @Value("${cluster.nodes}") String clusterNodes,
                       @Value("${server.port}") int serverPort) {
        this.nodeStateRepository = nodeStateRepository;
        this.transactionalRaftService = transactionalRaftService;
        this.nodeId = nodeId;
        this.clusterNodes = List.of(clusterNodes.split(","));
        this.ownNodeUrl = "localhost:" + serverPort;
        this.webClient = WebClient.create();
    }

    public Mono<Void> initializeNode() {
        log.info("Initializing node {}", nodeId);
        return nodeStateRepository.findByNodeId(nodeId).switchIfEmpty(Mono.defer(() -> {
                    NodeStateEntity node = new NodeStateEntity();
                    node.setNodeId(nodeId);
                    node.setState("FOLLOWER");
                    node.setCurrentTerm(0);
                    return transactionalRaftService.saveNodeState(node);
                }))
                .flatMap(node -> {
                    if (!node.getState().equals("LEADER")) {
                        return checkClusterReadiness().then();
                    }
                    return Mono.empty();
                });
    }

    private Mono<Boolean> checkClusterReadiness() {
        return Flux.interval(Duration.ofSeconds(5)).flatMap(tick -> isLeader().flatMap(isLeader -> {
            if (!isLeader) {
                return Flux.fromIterable(clusterNodes).flatMap(nodeUrl -> webClient.get().uri("http://" + nodeUrl + "/raft/status").retrieve().bodyToMono(NodeStatusDTO.class).map(dto -> {
                    dto.setNodeUrl(nodeUrl);
                    return dto;
                }).onErrorResume(e -> {
                    log.error("Error during status request to {}: {}", nodeUrl, e.getMessage());
                    // If the node is DOWN, create a DTO with DOWN status
                    return Mono.just(new NodeStatusDTO(nodeUrl, "DOWN", 0, "None", nodeUrl));
                })).collectList().flatMap(responses -> {
                    // Check if there is already a leader
                    boolean leaderExists = responses.stream().anyMatch(status -> "LEADER".equals(status.getState()));
                    if (leaderExists) {
                        // If another leader exists, ensure this node is not a leader
                        return nodeStateRepository.findByNodeId(nodeId).flatMap(node -> {
                            if ("LEADER".equals(node.getState())) {
                                return transactionalRaftService.stepDown(node);
                            }
                            return Mono.just(true);
                        });
                    }
                    if (electionInProgress.compareAndSet(false, true)) {
                        return startElection().thenReturn(true);
                    }
                    return Mono.just(false);
                });
            }
            return Mono.just(true);
        })).takeUntil(isReady -> (boolean) isReady).then(Mono.just(true));
    }

    public Mono<Void> startElection() {
        log.info("Node {} has started an election", nodeId);
        return nodeStateRepository.findByNodeId(nodeId).flatMap(node -> {
            node.setState("CANDIDATE");
            node.setCurrentTerm(node.getCurrentTerm() + 1);
            node.setVotedFor(nodeId);
            log.debug("Node {} increments term to {}", nodeId, node.getCurrentTerm());
            return transactionalRaftService.saveNodeState(node)
                    .flatMap(this::sendRequestVoteToOtherNodes);
        });
    }

    private Mono<Void> sendRequestVoteToOtherNodes(NodeStateEntity node) {
        log.info("Node {} has started the election for term {}", nodeId, node.getCurrentTerm());
        return Flux.fromIterable(clusterNodes).flatMap(otherNode -> {
            // Skip sending to self
            if (otherNode.equals(ownNodeUrl)) {
                return Mono.empty();
            }
            Map<String, Object> voteRequest = Map.of("candidateId", node.getNodeId(), "candidateTerm", node.getCurrentTerm());
            log.debug("Sending vote request to {}", otherNode);
            return webClient.post().uri("http://" + otherNode + "/raft/request-vote")
                    .bodyValue(voteRequest)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .doOnNext(voteGranted -> log.debug("Vote granted from {}: {}", otherNode, voteGranted))
                    .onErrorResume(e -> {
                        log.error("Error during vote request to {}: {}", otherNode, e.getMessage());
                        return Mono.just(false);
                    });
        }).collectList().flatMap(votes -> {
            log.debug("Votes received: {}", votes);
            // Include the vote from the node itself
            long positiveVotes = votes.stream().filter(v -> v).count() + 1;
            log.info("Node {} has received {} positive votes", nodeId, positiveVotes);
            if (positiveVotes > clusterNodes.size() / 2) {
                return transactionalRaftService.becomeLeader(node);
            }
            // Reset electionInProgress flag if not becoming leader
            log.debug("Node {} did not receive enough votes to become leader", nodeId);
            electionInProgress.set(false);
            return Mono.empty();
        }).then();
    }

    public Mono<Void> receiveHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
        log.debug("Received heartbeat from leader");
        return isLeader().flatMap(isLeader -> {
            if (isLeader) {
                log.warn("Leader {} received heartbeat from another leader. Stepping down.", nodeId);
                return nodeStateRepository.findByNodeId(nodeId)
                        .flatMap(transactionalRaftService::stepDown);
            }
            return Mono.empty();
        });
    }


    @PostConstruct
    public void monitorHeartbeats() {
        Flux.interval(Duration.ofSeconds(1)).flatMap(tick -> isLeader().flatMap(isLeader -> {
            if (!isLeader) {
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat > randomizedTimeout() && !electionInProgress.get()) { // Randomized timeout
                    return startElection();
                }
            }
            return Mono.empty();
        })).subscribe();
    }

    private long randomizedTimeout() {
        return 1500 + (long) (Math.random() * 1500);
    }

    public Mono<Boolean> requestVote(String candidateId, int candidateTerm) {
        log.debug("Received vote request from {} with term {}", candidateId, candidateTerm);
        return nodeStateRepository.findByNodeId(nodeId).flatMap(node -> {
            if (candidateTerm > node.getCurrentTerm()) {
                node.setCurrentTerm(candidateTerm);
                node.setVotedFor(candidateId);
                node.setState("FOLLOWER");
                log.debug("Voted in favor of {} for higher term {}", candidateId, candidateTerm);
                return transactionalRaftService.saveNodeState(node)
                        .thenReturn(true);
            } else if (candidateTerm == node.getCurrentTerm() && (node.getVotedFor() == null || node.getVotedFor().equals(candidateId))) {
                node.setVotedFor(candidateId);
                node.setState("FOLLOWER");
                log.debug("Voted in favor of {} for current term {}", candidateId, candidateTerm);
                return transactionalRaftService.saveNodeState(node)
                        .thenReturn(true);
            }
            log.debug("Voted against {} for term {}", candidateId, candidateTerm);
            return Mono.just(false);
        });
    }


    private Mono<Boolean> isLeader() {
        return nodeStateRepository.findByNodeId(nodeId).map(node -> {
            boolean leader = node.getState().equals("LEADER");
            log.debug("isLeader() for {}: {}", nodeId, leader);
            return leader;
        }).defaultIfEmpty(false);
    }


    public Mono<List<NodeStatusDTO>> getAllNodeStatuses() {
        return Flux.fromIterable(clusterNodes).flatMap(nodeUrl -> {
            if (nodeUrl.equals(ownNodeUrl)) {
                // Get status from local database
                return nodeStateRepository.findByNodeId(nodeId).map(node -> new NodeStatusDTO(node.getNodeId(), node.getState(), node.getCurrentTerm(), node.getVotedFor(), nodeUrl))
                        .onErrorResume(e -> {
                            log.error("Error retrieving local state: {}", e.getMessage());
                            // If it fails, consider the node as DOWN
                            return Mono.just(new NodeStatusDTO(nodeId, "DOWN", 0, "None", nodeUrl));
                        });
            } else {
                // Request status from other nodes
                return webClient.get().uri("http://" + nodeUrl + "/raft/status").retrieve().bodyToMono(NodeStatusDTO.class).map(dto -> {
                    dto.setNodeUrl(nodeUrl); // Set nodeUrl in DTO
                    return dto;
                }).onErrorResume(e -> {
                    log.error("Error fetching status from {}: {}", nodeUrl, e.getMessage());
                    // Assign nodeUrl as identifier if nodeId cannot be obtained
                    return Mono.just(new NodeStatusDTO(nodeUrl, "DOWN", 0, "None", nodeUrl));
                });
            }
        }).collectList();
    }

    public Mono<NodeStateEntity> getNodeStatusEntity() {
        log.debug("Status request received for node {}", nodeId);
        return nodeStateRepository.findByNodeId(nodeId).switchIfEmpty(Mono.error(new NodeStateNotFoundException("Node state not found")));
    }
}
