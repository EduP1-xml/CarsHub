package carshub.com.br.backend.services.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenha extends EmailService {

    private final JavaMailSender mailSender;

    public EmailSenha(JavaMailSender mailSender) {
        super(mailSender);
        this.mailSender = mailSender;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }


    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage mimeMessage = getMailSender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Novo Template HTML (CarsHub - Tema Carros Antigos)
            String htmlBody = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recuperação de Chaves</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;1,600&family=Lora:wght@400;500&display=swap');
                    
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #121212; /* Asfalto/Chumbo escuro */
                        font-family: 'Lora', serif;
                        color: #e6e6e6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .email-wrapper {
                        width: 100%;
                        background-color: #121212;
                        padding: 40px 0;
                    }
                    .email-content {
                        max-width: 500px;
                        margin: 0 auto;
                        background-color: #1c1c1c; /* Fundo do card */
                        border: 1px solid #333333;
                        border-top: 4px solid #d4af37; /* Dourado Clássico */
                        border-radius: 8px;
                        box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.8);
                        overflow: hidden;
                    }
                    .email-header {
                        padding: 32px 24px 16px;
                        text-align: center;
                    }
                    .email-header h1 {
                        margin: 0;
                        font-family: 'Playfair Display', serif;
                        font-size: 32px;
                        font-weight: 700;
                        color: #ffffff;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .email-header h1 span {
                        color: #d4af37;
                        font-style: italic;
                    }
                    .email-body {
                        padding: 20px 32px 40px;
                        text-align: center;
                    }
                    .icon-lock {
                        display: inline-block;
                        width: 56px;
                        height: 56px;
                        background-color: rgba(212, 175, 55, 0.1);
                        border: 1px solid rgba(212, 175, 55, 0.3);
                        border-radius: 50%;
                        line-height: 54px;
                        color: #d4af37;
                        font-size: 28px;
                        margin-bottom: 24px;
                    }
                    h2 {
                        margin: 0 0 16px;
                        font-family: 'Playfair Display', serif;
                        font-size: 24px;
                        color: #ffffff;
                        font-weight: 700;
                    }
                    p {
                        margin: 0 0 24px;
                        font-size: 16px;
                        line-height: 1.7;
                        color: #b3b3b3;
                    }
                    .verification-code {
                        background: linear-gradient(145deg, #161616, #222222);
                        border: 1px solid #d4af37;
                        border-radius: 6px;
                        padding: 20px;
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 32px;
                        font-weight: 700;
                        color: #d4af37;
                        letter-spacing: 10px;
                        margin: 24px 0;
                        display: block;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.5);
                        box-shadow: inset 0 2px 10px rgba(0,0,0,0.5);
                    }
                    .email-footer {
                        background-color: #161616;
                        padding: 24px;
                        text-align: center;
                        border-top: 1px solid #2a2a2a;
                    }
                    .footer-text {
                        font-size: 12px;
                        color: #777777;
                        margin: 0;
                        line-height: 1.5;
                        font-family: sans-serif;
                    }
                    @media only screen and (max-width: 600px) {
                        .email-content { width: 100% !important; border-radius: 0 !important; border-left: none; border-right: none; }
                        .email-wrapper { padding: 0 !important; }
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="email-content">
                        <div class="email-header">
                            <h1>Cars<span>Hub</span></h1>
                        </div>
                        
                        <div class="email-body">
                            <!-- Ícone de chave clássica -->
                            <div class="icon-lock">&#128477;</div> 
                            
                            <h2>Manutenção de Chaves</h2>
                            <p>Olá! Parece que você perdeu as chaves do seu clássico. Recebemos uma solicitação para resetar a senha da sua conta CarsHub. Use o código abaixo para forjar uma nova chave de acesso:</p>
                            
                            <div class="verification-code">{{CODIGO}}</div>
                            
                            <p style="font-size: 14px; margin-bottom: 0;">Este código de emergência expira em 10 minutos.</p>
                        </div>
                        
                        <div class="email-footer">
                            <p class="footer-text">Se você não solicitou a troca de chaves, ignore esta mensagem. Seu veículo continua seguro e trancado.</p>
                            <p class="footer-text" style="margin-top: 8px;">&copy; 2026 CarsHub. O clássico nunca morre.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;

            // Substitui o placeholder {{CODIGO}} pelo código real gerado
            String finalHtml = htmlBody.replace("{{CODIGO}}", code);

            helper.setText(finalHtml, true); // 'true' habilita o modo HTML
            helper.setTo(to);
            helper.setSubject("🗝️ Seu código de recuperação de chaves - CarsHub: " + code);
            helper.setFrom("CarsHub <noreply@carshub.com.br>");

            getMailSender().send(mimeMessage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao enviar e-mail HTML", e);
        }
    }
}