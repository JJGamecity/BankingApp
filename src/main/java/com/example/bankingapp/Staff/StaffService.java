package com.example.bankingapp.Staff;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Sort;
import java.util.List;

@Service
public class StaffService {
    private final StaffRepository repository;
    private final PasswordEncoder encoder;
    private final LoginNamePolicy loginNames;

    public StaffService(StaffRepository repository, PasswordEncoder encoder, LoginNamePolicy loginNames) {
        this.repository = repository;
        this.encoder = encoder;
        this.loginNames = loginNames;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse create(StaffRequest request) {
        loginNames.requireAvailable(request.username(), null);
        var account = new StaffAccount(request.username(), encoder.encode(request.password()), request.role());
        account.recordCreator(SecurityContextHolder.getContext().getAuthentication().getName());
        var staff = repository.saveAndFlush(account);
        return new StaffResponse(staff.getId(), staff.getUsername(), staff.getRole());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffResponse> list() {
        return repository.findAll(Sort.by("id")).stream()
                .map(staff -> new StaffResponse(staff.getId(), staff.getUsername(), staff.getRole())).toList();
    }
}
