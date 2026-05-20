package com.tibet.tourism.common.api;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@PreAuthorize("isAuthenticated()")
public class TestController {
    
    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public endpoint works!";
    }
}
