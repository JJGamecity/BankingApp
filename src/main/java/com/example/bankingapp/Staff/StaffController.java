package com.example.bankingapp.Staff;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/staff")
public class StaffController {
    private final StaffService service;

    public StaffController(StaffService service) { this.service = service; }

    @GetMapping
    public List<StaffResponse> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}
