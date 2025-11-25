package com.mp.service;

import com.mp.dto.UserDTO;
import com.mp.entity.Institution;
import com.mp.entity.Role;
import com.mp.entity.User;
import com.mp.exception.ResourceNotFoundException;
import com.mp.repository.InstitutionRepository; // ✅ Import
import com.mp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository; // ✅ Inject
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, InstitutionRepository institutionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private UserDTO toDto(User u) {
        return UserDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .roles(u.getRoles() != null ? u.getRoles().stream().map(Enum::name).collect(Collectors.toSet()) : null)
                .userType(u.getUserType())
                // ✅ Fix: Extract ID from the object if it exists
                .institutionId(u.getInstitution() != null ? u.getInstitution().getId() : null)
                .active(u.isActive())
                .emailVerified(u.isEmailVerified())
                .phone(u.getPhone())
                .profileImageUrl(u.getProfileImageUrl())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    public java.util.List<UserDTO> findAll() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDTO findById(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return toDto(u);
    }

    public UserDTO create(String email, String name, String rawPassword, Set<Role> roles, String userType, Long institutionId) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        User.UserBuilder userBuilder = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(rawPassword))
                .roles(roles)
                .userType(userType)
                .active(true)
                .emailVerified(false);

        // ✅ Fix: Find Institution and set it
        if (institutionId != null) {
            Institution inst = institutionRepository.findById(institutionId).orElse(null);
            userBuilder.institution(inst);
        }

        User saved = userRepository.save(userBuilder.build());
        return toDto(saved);
    }

    public UserDTO update(Long id, UserDTO dto) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        if (dto.getName() != null) u.setName(dto.getName());
        if (dto.getUserType() != null) u.setUserType(dto.getUserType());
        
        // ✅ Fix: Update Institution Relationship
        if (dto.getInstitutionId() != null) {
            Institution inst = institutionRepository.findById(dto.getInstitutionId()).orElse(null);
            u.setInstitution(inst);
        }

        if (dto.getPhone() != null) u.setPhone(dto.getPhone());
        if (dto.getProfileImageUrl() != null) u.setProfileImageUrl(dto.getProfileImageUrl());

        if (dto.isActive() != u.isActive()) u.setActive(dto.isActive());

        if (dto.getRoles() != null) {
            u.setRoles(dto.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet()));
        }
        User updated = userRepository.save(u);
        return toDto(updated);
    }

    public void delete(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        userRepository.delete(u);
    }
}