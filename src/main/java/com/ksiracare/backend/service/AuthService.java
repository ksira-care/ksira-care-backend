package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.dto.response.LoginResponseDto;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.repository.TherapistRepository;
import com.ksiracare.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TherapistRepository therapistRepository;
    private final JwtService jwtService;

    public LoginResponseDto login(LoginRequestDto request) {
        // This will verify the password automatically against the encrypted one in DB
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Therapist therapist = therapistRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        String token = jwtService.generateToken(therapist.getEmail(), therapist.getId());

        return LoginResponseDto.builder()
                .token(token)
                .therapistId(therapist.getId())
                .email(therapist.getEmail())
                .build();
    }
}