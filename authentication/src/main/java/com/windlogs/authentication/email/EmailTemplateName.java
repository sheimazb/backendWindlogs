package com.windlogs.authentication.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("""
            <!DOCTYPE html>
                       <html lang="fr">
                       <head>
                         <meta charset="UTF-8" />
                         <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                         <title>Activation du Compte - WindLogs</title>
                         <style>
                           :root {
                             --primary-color: #0a2559;
                             --secondary-color: #f7f9fc;
                             --text-color: #333;
                             --muted-color: #777;
                             --border-color: #e1e1e1;
                           }
            
                           body {
                             margin: 0;
                             padding: 0;
                             font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                             background-color: #f9f9f9;
                             color: var(--text-color);
                             line-height: 1.5;
                             font-size: 14px;
                           }
            
                           .container {
                             max-width: 500px;
                             margin: 20px auto;
                             background-color: #fff;
                             border: 1px solid var(--border-color);
                             border-radius: 6px;
                             overflow: hidden;
                             box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
                           }
            
                           .header {
                             background-color: var(--primary-color);
                             padding: 14px;
                             text-align: center;
                           }
            
                           .logo {
                             max-width: 40px;
                             height: auto;
                           }
            
                           .content {
                             padding: 20px;
                           }
            
                           .title {
                             font-size: 18px;
                             font-weight: 600;
                             color: var(--primary-color);
                             margin-bottom: 16px;
                           }
            
                           .message {
                             font-size: 13px;
                             margin-bottom: 14px;
                             color: #444;
                           }
            
                           .code-container {
                             text-align: center;
                             margin: 20px 0;
                           }
            
                           .verification-code {
                             display: inline-block;
                             padding: 10px 25px;
                             background-color: var(--secondary-color);
                             border: 1px solid #dbe3ec;
                             border-radius: 5px;
                             font-family: 'Courier New', monospace;
                             font-size: 18px;
                             font-weight: bold;
                             letter-spacing: 3px;
                             color: var(--primary-color);
                           }
            
                           .notice {
                             font-size: 12px;
                             color: var(--muted-color);
                             text-align: center;
                             margin-top: 8px;
                           }
            
                           .divider {
                             border-top: 1px solid #eaeaea;
                             margin: 20px 0;
                           }
            
                           .footer {
                             background-color: #f4f4f4;
                             padding: 12px 15px;
                             font-size: 11px;
                             color: var(--muted-color);
                             text-align: center;
                             border-top: 1px solid var(--border-color);
                           }
            
                           .company-name {
                             font-weight: bold;
                             color: var(--primary-color);
                           }
            
                           .contact-link {
                             color: #0056b3;
                             text-decoration: none;
                           }
            
                           .contact-link:hover {
                             text-decoration: underline;
                           }
            
                           .disclaimer {
                             font-size: 10px;
                             color: #999;
                             margin-top: 8px;
                           }
            
                           @media (max-width: 540px) {
                             .container {
                               margin: 15px;
                             }
                             .content {
                               padding: 16px;
                             }
                             .verification-code {
                               padding: 8px 20px;
                               font-size: 16px;
                             }
                           }
                         </style>
                       </head>
                       <body>
                         <div class="container">
                          <div class="header" style="background-color: #0a2559; padding: 14px; text-align: center;">
                             <img
                               src="https://png.pngtree.com/png-vector/20220519/ourmid/pngtree-white-triangle-shape-realistic-button-png-image_4689259.png"
                               alt="Logo WindLogs"
                               class="logo"
                             />
                           </div>
            
                           <div class="content">
                             <h1 class="title">Activation du Compte</h1>
            
                             <p class="message">%1$s</p>
            
                             <p class="message">
                               Merci pour votre inscription à <strong>WindLogs</strong>. Utilisez le code ci-dessous pour activer votre compte :
                             </p>
            
                             <div class="code-container">
                               <div class="verification-code">%3$s</div>
                             </div>
            
                             <p class="notice">Ce code expirera dans 15 minutes.</p>
            
                             <div class="divider"></div>
            
                             <p class="message">
                               Si vous n'avez pas initié cette demande, veuillez ignorer cet email.
                             </p>
                           </div>
            
                           <div class="footer">
                             <div>
                               <span class="company-name">WindLogs</span> |
                               <a href="mailto:wind-consulting@contact.com" class="contact-link">wind-consulting@contact.com</a>
                             </div>
            
                             <div class="disclaimer">
                               Ce message est confidentiel. Si vous n'êtes pas le destinataire prévu, toute diffusion est interdite.<br />
                               © 2025 WindLogs. Tous droits réservés.
                             </div>
                           </div>
                         </div>
                       </body>
                       </html>
            
        """),

    EMPLOYEE_CREDENTIALS("""
           <!DOCTYPE html>
                       <html lang="fr">
                       <head>
                         <meta charset="UTF-8" />
                         <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                         <title>WindLogs Account Credentials</title>
                         <style>
                           :root {
                             --primary-color: #0a2559;
                             --secondary-color: #f7f9fc;
                             --text-color: #333;
                             --muted-color: #777;
                             --border-color: #e1e1e1;
                           }
            
                           body {
                             font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                             background-color: var(--secondary-color);
                             margin: 0;
                             padding: 0;
                             color: var(--text-color);
                             line-height: 1.5;
                             font-size: 14px;
                           }
            
                           .container {
                             max-width: 500px;
                             margin: 20px auto;
                             background: #fff;
                             border-radius: 8px;
                             overflow: hidden;
                             box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
                             border: 1px solid var(--border-color);
                           }
            
                           .header {
                             background-color: var(--primary-color);
                             padding: 16px;
                             text-align: center;
                           }
            
                           .logo {
                             max-width: 40px;
                             height: auto;
                           }
            
                           .content {
                             padding: 25px 20px;
                           }
            
                           h2 {
                             color: var(--primary-color);
                             font-size: 20px;
                             font-weight: 600;
                             text-align: center;
                             margin: 0 0 15px;
                           }
            
                           .credentials {
                             background: #f1f5f9;
                             border-left: 4px solid #2563eb;
                             padding: 15px;
                             border-radius: 8px;
                           }
            
                           .field {
                             display: flex;
                             margin: 10px 0;
                             font-size: 14px;
                           }
            
                           .field-label {
                             font-weight: 600;
                             color: #64748b;
                             width: 90px;
                           }
            
                           .field-value {
                             color: #1e293b;
                             font-weight: 500;
                             word-break: break-word;
                           }
            
                           .note {
                             margin-top: 20px;
                             background: #fffbeb;
                             border-left: 4px solid #f59e0b;
                             padding: 10px;
                             border-radius: 6px;
                             font-size: 13px;
                             color: #92400e;
                           }
            
                           .footer {
                             background-color: #f4f4f4;
                             padding: 12px 15px;
                             font-size: 12px;
                             color: var(--muted-color);
                             text-align: center;
                             border-top: 1px solid var(--border-color);
                           }
            
                           .footer .company-name {
                             font-weight: bold;
                             color: var(--primary-color);
                           }
            
                           .contact-link {
                             color: #0056b3;
                             text-decoration: none;
                           }
            
                           .contact-link:hover {
                             text-decoration: underline;
                           }
            
                           @media (max-width: 540px) {
                             .container {
                               margin: 15px;
                             }
                             .content {
                               padding: 16px;
                             }
                           }
                         </style>
                       </head>
                       <body>
                         <div class="container">
                         <div class="header" style="background-color: #0a2559; padding: 14px; text-align: center;">
                             <img class="logo" src="https://png.pngtree.com/png-vector/20220519/ourmid/pngtree-white-triangle-shape-realistic-button-png-image_4689259.png" alt="WindLogs" />
                           </div>
            
                           <div class="content">
                             <h2>Account Credentials</h2>
                            \s
                        <span>Dear ${fullName}, </span>
                         <p>Your account has been created by ${partnerName}. Here are your login credentials:</p>
                                                 \s
                             <div class="credentials">
                               <div class="field">
                                 <div class="field-label">Username:</div>
                                 <div class="field-value">${email}</div>
                               </div>
                               <div class="field">
                                 <div class="field-label">Password:</div>
                                 <div class="field-value">${password}</div>
                               </div>
                                <div class="field">
                                 <div class="field-label">ROLE:</div>
                                 <div class="field-value">${role}}</div>
                               </div>
                             </div>
            
                             <div class="note">
                               Please change your password upon first login for security reasons.
                             </div>
                           </div>
            
                           <div class="footer">
                             <div>
                               <span class="company-name">WindLogs</span> |
                               <a href="mailto:wind-consulting@contact.com" class="contact-link">wind-consulting@contact.com</a>
                             </div>
                             <div style="font-size: 11px; margin-top: 8px;">
                               Ce message est confidentiel. Si vous n'êtes pas le destinataire prévu, toute diffusion est interdite.<br />
                               © 2025 WindLogs. Tous droits réservés.
                             </div>
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
                                                  :root {
                                                    --primary-color: #0a2559;
                                                    --secondary-color: #f7f9fc;
                                                    --text-color: #333;
                                                    --muted-color: #777;
                                                    --border-color: #e1e1e1;
                                                  }
                                                  .classh2{
                                                   text-align: center; \s
                                                  }
                                  \s
                                                  body {
                                                    margin: 0;
                                                    padding: 0;
                                                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                                    background-color: #f9f9f9;
                                                    color: var(--text-color);
                                                    line-height: 1.5;
                                                    font-size: 14px;
                                                  }
                                  \s
                                                  .container {
                                                    max-width: 500px;
                                                    margin: 20px auto;
                                                    background-color: #fff;
                                                    border: 1px solid var(--border-color);
                                                    border-radius: 6px;
                                                    overflow: hidden;
                                                    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
                                                  }
                                  \s
                                                  .header {
                                                    background-color: var(--primary-color);
                                                    padding: 14px;
                                                    text-align: center;
                                                  }
                                  \s
                                                  .logo {
                                                    max-width: 40px;
                                                    height: auto;
                                                  }
                                  \s
                                                  .content {
                                                    padding: 20px;
                                                  \s
                                                  }
                                  \s
                                                  .title {
                                                    font-size: 18px;
                                                    font-weight: 600;
                                                    color: var(--primary-color);
                                                    margin-bottom: 16px;
                                                  }
                                  \s
                                                  .message {
                                                    font-size: 13px;
                                                    margin-bottom: 14px;
                                                    color: #444;
                                                  }
                                  \s
                                                  .code-container {
                                                    text-align: center;
                                                    margin: 20px 0;
                                                   \s
                                                  }
                                  \s
                                                  .verification-code {
                                                    display: inline-block;
                                                    padding: 10px 25px;
                                                    background-color: var(--secondary-color);
                                                    border: 1px solid #dbe3ec;
                                                    border-radius: 5px;
                                                    font-family: 'Courier New', monospace;
                                                    font-size: 18px;
                                                    font-weight: bold;
                                                    letter-spacing: 3px;
                                                    color: var(--primary-color);
                                                  }
                                  \s
                                                  .notice {
                                                    font-size: 12px;
                                                    color: var(--muted-color);
                                                    text-align: center;
                                                    margin-top: 8px;
                                                  }
                                  \s
                                                  .divider {
                                                    border-top: 1px solid #eaeaea;
                                                    margin: 20px 0;
                                                  }
                                  \s
                                                  .footer {
                                                    background-color: #f4f4f4;
                                                    padding: 12px 15px;
                                                    font-size: 11px;
                                                    color: var(--muted-color);
                                                    text-align: center;
                                                    border-top: 1px solid var(--border-color);
                                                  }
                                  \s
                                                  .company-name {
                                                    font-weight: bold;
                                                    color: var(--primary-color);
                                                  }
                                  \s
                                                  .contact-link {
                                                    color: #0056b3;
                                                    text-decoration: none;
                                                  }
                                  \s
                                                  .contact-link:hover {
                                                    text-decoration: underline;
                                                  }
                                  \s
                                                  .disclaimer {
                                                    font-size: 10px;
                                                    color: #999;
                                                    margin-top: 8px;
                                                  }
                                  \s
                                                  @media (max-width: 540px) {
                                                    .container {
                                                      margin: 15px;
                                                    }
                                                    .content {
                                                      padding: 16px;
                                                    }
                                                    .verification-code {
                                                      padding: 8px 20px;
                                                      font-size: 16px;
                                                    }
                                                  }
                           </style>
                       </head>
                       <body>
                           <div class="container">
                               <!-- Header with logo and blue background -->
                               <div class="header" style="background-color: #0a2559;  text-align: center;">
                                   <img
                                       src="https://png.pngtree.com/png-vector/20220519/ourmid/pngtree-white-triangle-shape-realistic-button-png-image_4689259.png"
                                       alt="WindLogs Logo"
                                       style="max-width: 60px; height: auto;"
                                   />
                               </div>
            
                               <div class="content">
                                   <h2 class="classh2">Password Reset Request</h2>
                                  \s
                                   <div class="message">
                                       <p>Hello %1$s,</p>
                                       <p>We received a request to reset your password. Here is your password reset code:</p>
                                   </div>
                                  \s
                                   <div class="code-container">
                                       <h2 class="code">%3$s</h2>
                                   </div>
                                
                                   <div class="warning">
                                       ⏱️ This code will expire in 15 minutes
                                   </div>
                                  \s
                                   <p class="message">If you didn't request this password reset, please ignore this email or contact support if you have concerns.</p>
                               </div>
                              \s
                               <div class="footer">
                                   <p>Best regards,<br>
                                   <span class="company">The WindLogs Team</span></p>
                               </div>
                           </div>
                       </body>
                       </html>
                      
            
        """),

    PROFILE_STATUS("""
           <!DOCTYPE html>
               <html lang="fr">
               <head>
                 <meta charset="UTF-8" />
                 <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                 <title>Statut du Compte - WindLogs</title>
                 <style>
                   :root {
                     --primary-color: #0a2559;
                     --secondary-color: #f7f9fc;
                     --text-color: #333;
                     --muted-color: #777;
                     --border-color: #e1e1e1;
                     --success-color: #28a745;
                     --pending-color: #ffc107;
                     --refused-color: #dc3545;
                   }
            
                   body {
                     margin: 0;
                     padding: 0;
                     font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                     background-color: #f9f9f9;
                     color: var(--text-color);
                     line-height: 1.5;
                   }
            
                   .container {
                     max-width: 500px;
                     margin: 20px auto;
                     background-color: #fff;
                     border: 1px solid var(--border-color);
                     border-radius: 6px;
                     overflow: hidden;
                     box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
                   }
            
                   .header {
                     background-color: var(--primary-color);
                     padding: 14px;
                     text-align: center;
                   }
            
                   .logo {
                     max-width: 40px;
                     height: auto;
                   }
            
                   .content {
                     padding: 20px;
                     text-align: center;
                   }
            
                   .title {
                     font-size: 18px;
                     font-weight: 600;
                     color: var(--primary-color);
                     margin-bottom: 16px;
                   }
            
                   .status-icon {
                     font-size: 40px;
                     margin-bottom: 10px;
                   }
            
                   .message {
                     font-size: 13px;
                     color: #444;
                     margin-bottom: 16px;
                   }
            
                   .status-box {
                     display: inline-block;
                     padding: 10px 20px;
                     border-radius: 5px;
                     font-weight: bold;
                     font-size: 15px;
                     margin-bottom: 20px;
                   }
            
                   .approved {
                     background-color: #e6f4ea;
                     color: var(--success-color);
                     border: 1px solid var(--success-color);
                   }
            
                   .pending {
                     background-color: #fff8e1;
                     color: var(--pending-color);
                     border: 1px solid var(--pending-color);
                   }
            
                   .refused {
                     background-color: #fdecea;
                     color: var(--refused-color);
                     border: 1px solid var(--refused-color);
                   }
            
                   .footer {
                     background-color: #f4f4f4;
                     padding: 12px 15px;
                     font-size: 11px;
                     color: var(--muted-color);
                     text-align: center;
                     border-top: 1px solid var(--border-color);
                   }
            
                   .company-name {
                     font-weight: bold;
                     color: var(--primary-color);
                   }
            
                   .disclaimer {
                     font-size: 10px;
                     color: #999;
                     margin-top: 8px;
                   }
                 </style>
               </head>
               <body>
                 <div class="container">
                   <div class="header" style="background-color: #0a2559; padding: 14px; text-align: center;">
                     <img
                       src="https://png.pngtree.com/png-vector/20220519/ourmid/pngtree-white-triangle-shape-realistic-button-png-image_4689259.png"
                       alt="Logo WindLogs"
                       class="logo"
                     />
                   </div>
            
                   <div class="content">
                      <h1 class="title"> ${accountStatusText}</h1> <!-- dynamic status text -->
                     <p class="message">
                     ${message}  </p>
            
                     <div class="status-box approved">${buttonText}</div> 
            
                     <p class="message">
                       Si vous avez des questions, n'hésitez pas à nous contacter.
                     </p>
                   </div>
            
                   <div class="footer">
                     <div>
                       <span class="company-name">WindLogs</span> |
                       <a href="mailto:wind-consulting@contact.com">wind-consulting@contact.com</a>
                     </div>
                     <div class="disclaimer">
                       Ce message est confidentiel. Si vous n'êtes pas le destinataire prévu, toute diffusion est interdite.<br />
                       © 2025 WindLogs. Tous droits réservés.
                     </div>
                   </div>
                 </div>
               </body>
               </html>
            
        """);

    private final String template;

    EmailTemplateName(String template) {
        this.template = template;
    }
}
