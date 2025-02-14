package com.windlogs.authentication.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("""
        <html>
            <body>
                <h1>Bonjour ${fullName},</h1>
                <p>Merci de vous être inscrit. Pour activer votre compte, veuillez cliquer sur le lien suivant :</p>
                <p><a href="${activationUrl}?token=${token}">Activer mon compte</a></p>
                <p>Ce lien expirera dans 15 minutes.</p>
                <p>Cordialement,<br/>L'équipe Windlogs</p>
            </body>
        </html>
        """),

    EMPLOYEE_CREDENTIALS("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Your WindLogs Account Credentials</title>
        </head>
        <body>
            <h2>Welcome to WindLogs!</h2>
            
            <p>Dear ${fullName},</p>
            
            <p>Your account has been created by ${partnerName}. Here are your login credentials:</p>
            
            <ul>
                <li><strong>Email:</strong> ${email}</li>
                <li><strong>Password:</strong> ${password}</li>
                <li><strong>Role:</strong> ${role}</li>
            </ul>
            
            <p>For security reasons, please change your password after your first login.</p>
            
            <p>If you have any questions or concerns, please contact your partner administrator.</p>
            
            <p>Best regards,<br>
            The WindLogs Team</p>
        </body>
        </html>
        """);

    private final String template;

    EmailTemplateName(String template) {
        this.template = template;
    }
}
