package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.TherapistProfileDto;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.TherapistMapper;
import com.ksiracare.backend.repository.TherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final TherapistMapper therapistMapper;

    @Transactional(readOnly = true)
    public TherapistProfileDto getProfile(UUID therapistId) {
        return therapistRepository.findWithLanguagesById(therapistId)
                .map(therapistMapper::toProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found."));
    }
}
