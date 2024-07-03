# Raft Implementation with Spring Boot

### How to run

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8000 --node.id=node1 --cluster.nodes=localhost:8000,localhost:8001,localhost:8002"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8001 --node.id=node2 --cluster.nodes=localhost:8000,localhost:8001,localhost:8002"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8002 --node.id=node3 --cluster.nodes=localhost:8000,localhost:8001,localhost:8002"
```

### Curl options

- Start an election

```bash
curl -X POST http://localhost:8000/raft/start-election -H "Content-Type: application/json" -d '{"candidateId":"node1", "candidateTerm":1}'
```

- Initialize a node

```bash
curl -X POST http://localhost:8001/raft/initialize -H "Content-Type: application/json" -d '{"candidateId":"node1", "candidateTerm":1}'
```

- Request vote

```bash
curl -X POST http://localhost:8000/raft/request-vote -H "Content-Type: application/json" -d '{"candidateId":"node1", "candidateTerm":2}'
```