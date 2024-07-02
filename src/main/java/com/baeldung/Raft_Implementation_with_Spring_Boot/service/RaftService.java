package com.baeldung.Raft_Implementation_with_Spring_Boot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.baeldung.Raft_Implementation_with_Spring_Boot.model.NodeStateEntity;
import com.baeldung.Raft_Implementation_with_Spring_Boot.repository.NodeStateRepository;
import com.baeldung.Raft_Implementation_with_Spring_Boot.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RaftService {
    private final NodeStateRepository nodeStateRepository;
    private final WebClient webClient;

    private final String nodeId;
    private final List<String> clusterNodes;
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private final String ownNodeUrl;
    private volatile long lastHeartbeat = System.currentTimeMillis();
    private Disposable heartbeatTask;

    public RaftService(NodeStateRepository nodeStateRepository, LogEntryRepository logEntryRepository,
                       @Value("${node.id}") String nodeId,
                       @Value("${cluster.nodes}") String clusterNodes,
                       @Value("${server.port}") int serverPort) {
        this.nodeStateRepository = nodeStateRepository;
        this.nodeId = nodeId;
        this.clusterNodes = List.of(clusterNodes.split(","));
        this.ownNodeUrl = "localhost:" + serverPort;
        this.webClient = WebClient.create();
    }

    @Transactional
    public Mono<Void> initializeNode() {
        System.out.println("Initializing node " + nodeId);
        return nodeStateRepository.findByNodeId(nodeId)
                .switchIfEmpty(Mono.defer(() -> {
                    NodeStateEntity node = new NodeStateEntity();
                    node.setNodeId(nodeId);
                    node.setState("FOLLOWER");
                    node.setCurrentTerm(0);
                    return nodeStateRepository.save(node)
                            .doOnSuccess(savedNode -> System.out.println("State of node " + nodeId + " initialized to FOLLOWER"));
                }))
                .flatMap(node -> {
                    if (!node.getState().equals("LEADER")) {
                        return checkClusterReadiness().then();
                    }
                    return Mono.empty();
                });
    }

    private Mono<Boolean> checkClusterReadiness() {
        System.out.println("Checking cluster readiness, node " + nodeId);
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick ->
                        isLeader()
                                .flatMap(isLeader -> {
                                    if (!isLeader) {
                                        return Flux.fromIterable(clusterNodes)
                                                .flatMap(nodeUrl ->
                                                        webClient.get()
                                                                .uri("http://" + nodeUrl + "/raft/status")
                                                                .retrieve()
                                                                .bodyToMono(String.class)
                                                                .onErrorResume(e -> Mono.empty())
                                                )
                                                .collectList()
                                                .flatMap(responses -> {
                                                    // Check if leader exist
                                                    boolean leaderExists = responses.stream().anyMatch(status -> status.equals("LEADER"));
                                                    if (!leaderExists && electionInProgress.compareAndSet(false, true)) {
                                                        return startElection().thenReturn(true);
                                                    }
                                                    return Mono.just(leaderExists);
                                                });
                                    }
                                    return Mono.just(true);
                                })
                )
                .takeUntil(isReady -> isReady)
                .then(Mono.just(true));
    }


    private Mono<Boolean> checkClusterSize() {
        // Check if the cluster is ready and check active nodes
        System.out.println("Checking cluster size, node " + nodeId);
        return Flux.fromIterable(clusterNodes)
                .flatMap(nodeUrl ->
                        webClient.get()
                                .uri("http://" + nodeUrl + "/raft/status")
                                .retrieve()
                                .bodyToMono(String.class)
                                .onErrorResume(e -> Mono.empty())
                )
                .collectList()
                .flatMap(responses -> {
                    if (responses.size() + 1 >= clusterNodes.size() + 1 && electionInProgress.compareAndSet(false, true)) {
                        return startElection().thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    @Transactional
    public Mono<Void> startElection() {
        // Set the election flag to true
        System.out.println("Node " + nodeId + " has started an election");
        return nodeStateRepository.findByNodeId(nodeId)
                .flatMap(node -> {
                    node.setState("CANDIDATE");
                    node.setCurrentTerm(node.getCurrentTerm() + 1);
                    node.setVotedFor(nodeId);

                    return nodeStateRepository.save(node)
                            .doOnSuccess(savedNode -> System.out.println("Node " + nodeId + " is CANDIDATE with term " + savedNode.getCurrentTerm()))
                            .flatMap(this::sendRequestVoteToOtherNodes);
                });
    }

    private Mono<Void> sendRequestVoteToOtherNodes(NodeStateEntity node) {
        System.out.println("Node " + nodeId + " has started an election with term " + node.getCurrentTerm());
        return Flux.fromIterable(clusterNodes)
                .flatMap(otherNode -> {
                    // Stop sending vote requests to self
                    if (otherNode.equals(ownNodeUrl)) {
                        return Mono.empty();
                    }
                    Map<String, Object> voteRequest = Map.of(
                            "candidateId", node.getNodeId(),
                            "candidateTerm", node.getCurrentTerm()
                    );
                    return webClient.post()
                            .uri("http://" + otherNode + "/raft/request-vote")
                            .bodyValue(voteRequest)
                            .retrieve()
                            .bodyToMono(Boolean.class)
                            .onErrorResume(e -> {
                                System.err.println("Error sending vote request to " + otherNode + ": " + e.getMessage());
                                return Mono.just(false);
                            });
                })
                .collectList()
                .flatMap(votes -> {
                    // Include the vote of the current node
                    long positiveVotes = votes.stream().filter(v -> v).count() + 1;
                    System.out.println("Node " + nodeId + " has received " + positiveVotes + " votes");
                    if (positiveVotes > clusterNodes.size() / 2) {
                        return becomeLeader();
                    }
                    // Reset the election flag
                    electionInProgress.set(false);
                    return Mono.empty();
                });
    }

    private Mono<Void> becomeLeader() {
        System.out.println("Node " + nodeId + " is now the leader");
        return nodeStateRepository.findByNodeId(nodeId)
                .flatMap(node -> {
                    node.setState("LEADER");
                    return nodeStateRepository.save(node)
                            .doOnSuccess(n -> System.out.println("Node " + nodeId + " is now the leader"));
                })
                .doOnSuccess(v -> startHeartbeat())
                .then();
    }


    private void startHeartbeat() {
        heartbeatTask = Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> sendHeartbeat())
                .subscribe();
    }

    private Mono<Void> sendHeartbeat() {
        return Flux.fromIterable(clusterNodes)
                .flatMap(otherNode -> {
                    // Stop sending heartbeats to self
                    if (otherNode.equals(ownNodeUrl)) {
                        return Mono.empty();
                    }
                    return webClient.post()
                            .uri("http://" + otherNode + "/raft/heartbeat")
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v ->
                                    {
                                    }
//                                    System.out.println("Heartbeat sent to " + otherNode)
                            )
                            .onErrorResume(e -> {
                                System.err.println("Error during heartbeat to " + otherNode + ": " + e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    public Mono<Void> receiveHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
//        System.out.println("Received heartbeat from " + nodeId);
        return Mono.empty();
    }

    @PostConstruct
    public void monitorHeartbeats() {
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick ->
                        isLeader()
                                .flatMap(isLeader -> {
                                    if (!isLeader) {
                                        long now = System.currentTimeMillis();
                                        if (now - lastHeartbeat > randomizedTimeout() && !electionInProgress.get()) { // Timeout randomizzato
                                            return startElection();
                                        }
                                    }
                                    return Mono.empty();
                                })
                )
                .subscribe();
    }

    private long randomizedTimeout() {
        // Generate a random timeout between 1500 and 3000 ms
        return 1500 + (long) (Math.random() * 1500);
    }

    public Mono<Boolean> requestVote(String candidateId, int candidateTerm) {
        System.out.println("Received vote request from " + candidateId + " with term " + candidateTerm);
        return nodeStateRepository.findByNodeId(nodeId)
                .flatMap(node -> {
                    if (candidateTerm > node.getCurrentTerm() ||
                            (candidateTerm == node.getCurrentTerm() &&
                                    (node.getVotedFor() == null || node.getVotedFor().equals(candidateId)))) {
                        node.setCurrentTerm(candidateTerm);
                        node.setVotedFor(candidateId);
                        node.setState("FOLLOWER"); // change state to follower
                        System.out.println("Vote in favor of " + candidateId + " for term " + candidateTerm);
                        return nodeStateRepository.save(node)
                                .doOnSuccess(savedNode -> System.out.println("State of node " + nodeId + " updated to FOLLOWER"))
                                .thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    private Mono<Boolean> isLeader() {
        return nodeStateRepository.findByNodeId(nodeId)
                .map(node -> {
                    boolean leader = node.getState().equals("LEADER");
                    System.out.println("isLeader() for " + nodeId + ": " + leader);
                    return leader;
                })
                .defaultIfEmpty(false);
    }

    public Mono<String> getNodeStatus() {
        System.out.println("Request for node status: " + nodeId);
        return nodeStateRepository.findByNodeId(nodeId)
                .map(NodeStateEntity::getState)
                .defaultIfEmpty("UNKNOWN");
    }
}