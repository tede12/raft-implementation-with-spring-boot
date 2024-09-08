package com.baeldung.Raft_Implementation_with_Spring_Boot.persistence.model;

public enum NodeState {
    FOLLOWER,
    LEADER,
    CANDIDATE,
    DOWN
}
