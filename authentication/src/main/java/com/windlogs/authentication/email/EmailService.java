package com.windlogs.authentication.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Original method for account activation
    public void sendEmail(
            String to,
            String name,
            EmailTemplateName template,
            String activationUrl,
            String token,
            String subject
    ) throws MessagingException {
        try {
            logger.info("Preparing to send email to: {}", to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            String htmlContent = String.format(
                    template.getTemplate(),
                    name,
                    activationUrl,
                    token
            );

            logger.debug("Email HTML content: {}", htmlContent);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error(" Failed to send email to: {} - Error: {}", to, e.getMessage());
            throw e;
        }
    }

    // New method for employee credentials
    public void sendEmployeeCredentialsEmail(
            String to,
            String subject,
            Map<String, String> variables
    ) throws MessagingException {
        try {
            logger.info("Preparing to send employee credentials email to: {}", to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            String htmlContent = EmailTemplateName.EMPLOYEE_CREDENTIALS.getTemplate();

            // Replace all variables in the template
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace(
                        "${" + entry.getKey() + "}",
                        entry.getValue()
                );
            }

            logger.debug("Email HTML content: {}", htmlContent);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info(" Employee credentials email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send employee credentials email to: {} - Error: {}", to, e.getMessage());
            throw e;
        }
    }
    public void sendPartnerProfileStatus(
            String to,
            String subject,
            boolean accountLocked,
            Map<String, String> variables
    ) throws MessagingException {
        try {
            logger.info("Preparing to send partner profile status to: {}", to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            // Charger le template
            String htmlContent = EmailTemplateName.PROFILE_STATUS.getTemplate();

            // Définir le statut du compte
            String accountStatusText = accountLocked ? "Votre compte est bloqué" : "Votre compte est actif";
            String accountStatusColor = accountLocked ? "red" : "green";
            String buttonText = accountLocked ? "Contacter le support" : "Accéder à mon compte";
            String buttonLink = accountLocked ? "mailto:support@yourdomain.com" : "https://yourwebsite.com/login";
            String message = accountLocked
                    ? "Nous avons détecté une activité inhabituelle sur votre compte et l'avons temporairement bloqué."
                    : "Bonne nouvelle ! Votre compte est actif et prêt à être utilisé.";

            // Ajouter les nouvelles variables
            variables.put("accountStatusText", accountStatusText);
            variables.put("accountStatusColor", accountStatusColor);
            variables.put("buttonText", buttonText);
            variables.put("buttonLink", buttonLink);
            variables.put("message", message);

            // Remplacement optimisé des variables
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            logger.debug("Email HTML content: {}", htmlContent);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info("Partner profile status email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send profile status email to: {} - Error: {}", to, e.getMessage());
            throw e;
        }
    }

}