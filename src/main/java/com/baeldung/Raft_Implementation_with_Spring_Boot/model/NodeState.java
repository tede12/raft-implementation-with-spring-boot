package com.baeldung.Raft_Implementation_with_Spring_Boot.model;

public enum NodeState {
    FOLLOWER,
    LEADER,
    CANDIDATE,
    DOWN
}
