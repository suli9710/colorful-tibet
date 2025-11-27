package com.tibet.tourism.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public endpoint works!";
    }
}
