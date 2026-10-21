package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AaptaMapper;
import com.ksiracare.backend.repository.AaptaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AaptaService {

    private final AaptaRepository aaptaRepository;
    private final AaptaMapper aaptaMapper;

    @Transactional(readOnly = true)
    public AaptaProfileDto getProfile(UUID aaptaId) {
        return aaptaRepository.findWithLanguagesById(aaptaId)
                .map(aaptaMapper::toProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Aapta not found."));
    }
}
