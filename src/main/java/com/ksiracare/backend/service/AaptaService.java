package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.UpdateAaptaRequestDto;
import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.exception.EmailTakenException;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AaptaMapper;
import com.ksiracare.backend.policy.AaptaProfileEditPolicy;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.SessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * An aapta's profile — one format whether the aapta views and edits it in their portal or an
 * admin does in the console, so the two always agree. Who may change which field is decided by
 * {@link AaptaProfileEditPolicy}.
 */
@Service
@RequiredArgsConstructor
public class AaptaService {

    private final AaptaRepository aaptaRepository;
    private final AccountDirectory accounts;
    private final LanguageRepository languageRepository;
    private final AaptaProfileEditPolicy editPolicy;
    private final AaptaMapper aaptaMapper;

    @Transactional(readOnly = true)
    public AaptaProfileDto getProfile(UUID aaptaId) {
        return aaptaMapper.toProfile(findActive(aaptaId));
    }

    /**
     * Applies the fields sent, if {@code editor} may change them. A new email signs the aapta out
     * everywhere, since it is their login.
     */
    @Transactional
    public AaptaProfileDto update(SessionPrincipal editor, UUID aaptaId, UpdateAaptaRequestDto request) {
        editPolicy.check(editor.role(), request.fieldsSent());
        Aapta aapta = findActive(aaptaId);

        if (request.firstName() != null) aapta.setFirstName(request.firstName().trim());
        if (request.middleName() != null) aapta.setMiddleName(blankToNull(request.middleName()));
        if (request.lastName() != null) aapta.setLastName(request.lastName().trim());
        if (request.phone() != null) aapta.setPhone(blankToNull(request.phone()));
        if (request.dateOfBirth() != null) aapta.setDateOfBirth(request.dateOfBirth());
        if (request.address() != null) aapta.setAddress(blankToNull(request.address()));
        if (request.languages() != null) aapta.setLanguages(languageRepository.findSet(request.languages()));
        if (request.email() != null) changeEmail(aapta, Account.normaliseEmail(request.email()));

        try {
            return aaptaMapper.toProfile(aaptaRepository.saveAndFlush(aapta));
        } catch (DataIntegrityViolationException raceWithAnotherEdit) {
            throw new EmailTakenException();
        }
    }

    private void changeEmail(Aapta aapta, String email) {
        if (email.equals(aapta.getEmail())) {
            return;
        }
        if (accounts.isEmailTaken(email, aapta.getId())) {
            throw new EmailTakenException();
        }
        aapta.setEmail(email);
        aapta.endAllSessions();
    }

    private Aapta findActive(UUID aaptaId) {
        return aaptaRepository.findWithLanguagesById(aaptaId)
                .filter(Aapta::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Aapta not found."));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
