package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.AdminProfileDto;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AdminMapper;
import com.ksiracare.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminProfileDto getProfile(UUID adminId) {
        return adminRepository.findById(adminId)
                .map(adminMapper::toProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));
    }
}
