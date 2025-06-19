package com.vengalsas.core.conciliation.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conciliation")
public class TestController {

    @GetMapping("/status")
    public String status() {
        return "API is running";
    }
}
