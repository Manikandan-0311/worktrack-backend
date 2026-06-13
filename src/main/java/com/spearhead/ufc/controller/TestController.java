package com.spearhead.ufc.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spearhead.ufc.config.JwtUtil;
import com.spearhead.ufc.service.AuthTokenService;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "http://localhost:8082") // Explicit for now
public class TestController {

    private final JwtUtil jwtUtil;

    public TestController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/dummy")
    public ResponseEntity<?> dummyApi() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "This is a protected dummy API");
        return ResponseEntity.ok(resp);
    }
}

