package com.windlogs.authentication.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        max-width: 400px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #ffffff;
                    }
                    .card {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        padding: 20px;
                        text-align: center;
                    }
                    .logo {
                        width: 120px;
                        margin: 20px auto;
                    }
                    .title {
                        font-size: 24px;
                        font-weight: 600;
                        color: #333;
                        margin: 15px 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                    }
                    .activation-code {
                        background: #0066FF;
                        color: white;
                        padding: 10px 20px;
                        border-radius: 6px;
                        font-size: 18px;
                        font-weight: 600;
                        margin: 20px 0;
                        display: inline-block;
                        text-decoration: none;
                    }
                    .expiry-text {
                        color: #FF3B30;
                        font-size: 14px;
                        margin: 10px 0;
                    }
                    .footer-note {
                        background: #F5F5F5;
                        border-radius: 8px;
                        padding: 15px;
                        margin-top: 20px;
                        font-size: 14px;
                        color: #666;
                        text-align: left;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <img src="data:image/png;base64,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" alt="Wind Consulting" class="logo">
                   \s
                    <div class="title">
                        Activation du Compte <span style="font-size: 24px;">😊</span>
                    </div>
                   \s
                    <div style="color: #333; margin: 15px 0;">
                        %1$s
                    </div>
                   \s
                    <p style="color: #444; margin: 15px 0;">
                        Merci de vous être inscrit. Pour activer votre compte, veuillez copier le code suivant :
                    </p>
                   \s
                    <a href="%2$s?token=%3$s" class="activation-code">
                        %3$s
                    </a>
                   \s
                    <p class="expiry-text">
                        Ce code expirera dans 15 minutes
                    </p>
                   \s
                    <div class="footer-note">
                        Cher utilisateur, Merci d'avoir vérifié votre compte d'entreprise avec WindLogs. Notre équipe espère que vous apprécierez notre service ! Si vous avez des questions, n'hésitez pas à nous contacter à wind-consulting@contact.com.<br/><br/>
                        Cordialement,<br/>
                        L'équipe WindLogs
                    </div>
                </div>
            </body>
            </html>
        """),

    EMPLOYEE_CREDENTIALS("""
         <!DOCTYPE html>
                               <html>
                               <head>
                                   <meta charset="UTF-8">
                                   <title>WindLogs Account Credentials</title>
                                   <style>
                                       body {
                                           font-family: Arial, sans-serif;
                                           max-width: 400px;
                                           margin: 20px auto;
                                           text-align: center;
                                           background-color: #f5f5f5;
                                       }
                                       .card {
                                           background: white;
                                           border-radius: 15px;
                                           padding: 25px;
                                           box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                       }
                                       .logo {
                                           width: 100px;
                                           margin-bottom: 20px;
                                       }
                                       .logo img {
                                           width: 100%;
                                       }
                                       h2 {
                                           color: #333;
                                           margin: 15px 0;
                                           font-size: 18px;
                                       }
                                       .credentials {
                                           text-align: left;
                                           margin: 20px 0;
                                       }
                                       .credentials p {
                                           margin: 5px 0;
                                       }
                                       .footer {
                                           color: #666;
                                           font-size: 14px;
                                           margin-top: 20px;
                                           border-top: 1px solid #eee;
                                           padding-top: 15px;
                                       }
                                       strong {
                                           color: #333;
                                       }
                                   </style>
                               </head>
                               <body>
                                   <div class="card">
                                       <div class="logo">
                                           <img src="wind-logo.png" alt="Wind Logo">
                                       </div>
            
                                       <h2>Account Credentials</h2>
            
                                       <div class="credentials">
                                           <p>Dear ${fullName},</p>
                                           <p>Your account has been created by ${partnerName}. Here are your login credentials:</p>
                                           <p><strong>EMAIL:</strong> ${email}</p>
                                           <p><strong>PASSWORD:</strong> ${password}</p>
                                           <p><strong>ROLE:</strong> ${role}</p>
                                       </div>
            
                                       <p>For security reasons, please change your password after your first login.</p>
            
                                       <p>If you have any questions or concerns, please contact your partner administrator.</p>
            
                                       <div class="footer">
                                           <p>Best regards,<br>
                                           The WindLogs Team</p>
                                       </div>
                                   </div>
                               </body>
                               </html>
        """),

    RESET_PASSWORD("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .card {
                        background: white;
                        border-radius: 8px;
                        padding: 20px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #0066FF;
                        text-align: center;
                        margin: 20px 0;
                        letter-spacing: 4px;
                    }
                    .warning {
                        color: #ff3b30;
                        font-size: 14px;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Password Reset Request</h2>
                    <p>Hello %1$s,</p>
                    <p>We received a request to reset your password. Here is your password reset code:</p>
                    
                    <div class="code">%3$s</div>
                    
                    <p>To reset your password, enter this code at %2$s</p>
                    
                    <p class="warning">This code will expire in 15 minutes.</p>
                    
                    <p>If you didn't request this password reset, please ignore this email or contact support if you have concerns.</p>
                    
                    <p>Best regards,<br>WindLogs Team</p>
                </div>
            </body>
            </html>
        """),
    PROFILE_STATUS("""
  <!DOCTYPE html>
                                   <html lang="fr">
                                   <head>
                                       <meta charset="UTF-8">
                                       <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                       <title>Statut de votre compte</title>
                                       <style>
                                           body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                                           .container { max-width: 500px; margin: 50px auto; background: #ffffff; padding: 20px; border-radius: 8px; text-align: center; }
                                           .header { background: ${accountStatusColor}; color: white; padding: 15px; font-size: 20px; font-weight: bold; border-radius: 8px 8px 0 0; }
                                           .message { font-size: 16px; color: #333; margin: 20px 0; }
                                           .btn { display: inline-block; padding: 10px 20px; background: ${accountStatusColor}; color: white; text-decoration: none; font-weight: bold; border-radius: 5px; }
                                           .btn:hover { background: darken(${accountStatusColor}, 10%); }
                                           .footer { margin-top: 20px; font-size: 12px; color: #777; }
                                       </style>
                                   </head>
                                   <body>
                                       <div class="container">
                                           <div class="header">${accountStatusText}</div>
                                           <p class="message">${message}</p>
                                           <a href="${buttonLink}" class="btn">${buttonText}</a>
                                           <p class="footer">© 2024 Votre Entreprise. Tous droits réservés.</p>
                                       </div>
                                   </body>
                                   </html>
            
            
""");

    private final String template;

    EmailTemplateName(String template) {
        this.template = template;
    }
}
