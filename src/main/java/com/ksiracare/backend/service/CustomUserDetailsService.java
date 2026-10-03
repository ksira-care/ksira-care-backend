package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.repository.TherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final TherapistRepository therapistRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Therapist therapist = therapistRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found with email: " + email));

        return new User(
                therapist.getEmail(),
                therapist.getPassword(), // Needs to be BCrypt encrypted in the DB
                therapist.isActive(),
                true, true, true,
                Collections.emptyList() // Add roles/authorities here if needed
        );
    }
}