package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.UserProfileDto.PartnerResponse;
import com.windlogs.authentication.email.EmailService;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.TokenRepository;
import com.windlogs.authentication.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final EmailService emailService;

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


    public User editPartnerStatus(String email, PartnerResponse request) {
        logger.info("Block/unblock request for user with email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for email: " + email
                ));

        if (request.getAccountLocked() != null) {
            boolean oldStatus = user.isAccountLocked();
            boolean newStatus = request.getAccountLocked();

            if (oldStatus != newStatus) {
                user.setAccountLocked(newStatus);
                logger.info("Account status updated for user: {}", email);

                Map<String, String> emailVariables = new HashMap<>();
                emailVariables.put("username", user.getUsername());
                emailVariables.put("email", user.getEmail());

                String subject = newStatus ? "Votre compte a été bloqué" : " Votre compte est activé";
                try {
                    emailService.sendPartnerProfileStatus(email, subject, newStatus, emailVariables);
                    logger.info("Notification email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send email to {}: {}", email, e.getMessage());
                }
            } else {
                logger.info("No changes in account status for user: {}", email);
            }
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deletePartner(String email) {
        logger.info("Delete request for user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for email: " + email
                ));
        tokenRepository.deleteByUser(user);
        userRepository.delete(user);
        logger.info("User with email {} successfully deleted.", email);
    }
}
