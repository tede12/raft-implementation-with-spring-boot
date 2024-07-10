package com.baeldung.Raft_Implementation_with_Spring_Boot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MonitorController {

    @GetMapping("/monitor")
    public String monitorPage() {
        return "monitor";
    }
}
