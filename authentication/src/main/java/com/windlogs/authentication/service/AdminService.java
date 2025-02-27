package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.UserProfileDto.PartnerResponse;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public List<PartnerResponse> fetchAllPartners() {
        List<User> partners = userRepository.findAllByRole(Role.PARTNER); // Ensure method returns List<User>
        return partners.stream()
                .map(this::convertToPartnerResponse)
                .collect(Collectors.toList()); // Collect the stream into a List
    }

    private PartnerResponse convertToPartnerResponse(User user) {
        return PartnerResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .image(user.getImage())
                .role(user.getRole())
                .phone(user.getPhone())
                .location(user.getLocation())
                .company(user.getCompany())
                .lien(user.getLien())
                .accountLocked(user.isAccountLocked())
                .build();
    }
}
