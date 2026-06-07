package com.tibet.tourism.modules.admin.web;

import com.tibet.tourism.modules.admin.application.AdminSecurityPostureService;
import com.tibet.tourism.modules.admin.web.dto.SecurityPostureResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSecurityPostureController {

    private final AdminSecurityPostureService securityPostureService;

    public AdminSecurityPostureController(AdminSecurityPostureService securityPostureService) {
        this.securityPostureService = securityPostureService;
    }

    @GetMapping("/security-posture")
    public ResponseEntity<SecurityPostureResponse> getSecurityPosture() {
        return ResponseEntity.ok(securityPostureService.getSecurityPosture());
    }
}
