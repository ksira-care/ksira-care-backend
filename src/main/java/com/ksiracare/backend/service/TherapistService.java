package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.TherapistResponseDto;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.TherapistMapper;
import com.ksiracare.backend.repository.TherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TherapistService {
    private final TherapistRepository therapistRepository;
    private final TherapistMapper therapistMapper;

    public TherapistResponseDto getTherapistById(UUID id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow( () ->
                        new ResourceNotFoundException("Therapist Not Found!")
                );
        return therapistMapper.therapistToDto(therapist);
    }
}
