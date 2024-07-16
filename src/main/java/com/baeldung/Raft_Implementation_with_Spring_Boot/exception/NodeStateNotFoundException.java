package com.baeldung.Raft_Implementation_with_Spring_Boot.exception;

public class NodeStateNotFoundException extends RuntimeException {
    public NodeStateNotFoundException(String message) {
        super(message);
    }
}
