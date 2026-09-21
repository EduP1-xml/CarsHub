package carshub.com.br.backend.services.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }


    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Template HTML - Tema Carros Antigos (Criação de Conta)
            String htmlBody = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Código de Ignição</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;1,600&family=Lora:wght@400;500&display=swap');
                    
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #121212; /* Fundo chumbo/asfalto */
                        font-family: 'Lora', serif;
                        color: #e6e6e6; 
                        -webkit-font-smoothing: antialiased;
                    }
                    .wrapper {
                        width: 100%;
                        table-layout: fixed;
                        background-color: #121212;
                        padding-bottom: 60px;
                    }
                    .main-table {
                        background-color: #121212;
                        margin: 0 auto;
                        width: 100%;
                        max-width: 600px;
                        border-spacing: 0;
                        color: #e6e6e6;
                    }
                    .logo-container {
                        padding: 40px 0 20px;
                        text-align: center;
                    }
                    .logo {
                        font-family: 'Playfair Display', serif;
                        font-size: 32px;
                        font-weight: 700;
                        color: #ffffff;
                        text-decoration: none;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .logo span {
                        color: #d4af37; /* Dourado Clássico */
                        font-style: italic;
                    }
                    .card {
                        background-color: #1c1c1c; /* Chumbo mais claro */
                        border: 1px solid #333333;
                        border-top: 4px solid #d4af37; /* Detalhe dourado no topo */
                        border-radius: 8px;
                        padding: 48px 40px;
                        text-align: center;
                        box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.8);
                    }
                    .icon-container {
                        margin-bottom: 24px;
                    }
                    .shield-icon {
                        display: inline-block;
                        width: 56px;
                        height: 56px;
                        background-color: rgba(212, 175, 55, 0.1);
                        border: 1px solid rgba(212, 175, 55, 0.3);
                        border-radius: 50%;
                        line-height: 54px;
                        color: #d4af37;
                        font-size: 28px;
                    }
                    .title {
                        font-family: 'Playfair Display', serif;
                        color: #ffffff;
                        font-size: 26px;
                        font-weight: 700;
                        margin: 0 0 16px;
                        letter-spacing: 0.5px;
                    }
                    .text {
                        color: #b3b3b3; 
                        font-size: 16px;
                        line-height: 1.7;
                        margin: 0 0 32px;
                    }
                    .code-container {
                        background: linear-gradient(145deg, #161616, #222222);
                        border: 1px solid #d4af37; 
                        border-radius: 6px;
                        padding: 24px;
                        margin: 0 0 32px;
                        box-shadow: inset 0 2px 10px rgba(0,0,0,0.5);
                    }
                    .code {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 34px;
                        font-weight: 700;
                        color: #d4af37;
                        letter-spacing: 12px;
                        margin: 0;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.5);
                    }
                    .warning-text {
                        font-size: 13px;
                        color: #666666; 
                        margin: 0;
                        font-family: sans-serif;
                    }
                    .footer {
                        padding-top: 32px;
                        text-align: center;
                        font-family: sans-serif;
                    }
                    .footer-text {
                        color: #777777; 
                        font-size: 12px;
                        margin: 4px 0;
                    }
                    .footer-link {
                        color: #d4af37;
                        text-decoration: none;
                    }
                    
                    /* Responsividade */
                    @media screen and (max-width: 600px) {
                        .card {
                            padding: 32px 20px;
                            border-radius: 0;
                            border-left: none;
                            border-right: none;
                        }
                        .code {
                            font-size: 28px;
                            letter-spacing: 6px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <table class="main-table" role="presentation">
                        <tr>
                            <td class="logo-container">
                                <a href="#" class="logo">Cars<span>Hub</span></a>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <div class="card">                                  
                                    <div class="icon-container">
                                        <!-- Ícone de carro clássico -->
                                        <span class="shield-icon">&#128662;</span>
                                    </div>
                                    
                                    <h1 class="title">Bem-vindo(a) à Garagem!</h1>
                                    
                                    <p class="text">
                                        Os motores estão aquecendo! Para concluir sua matrícula e ter acesso ao acervo exclusivo de clássicos, por favor, dê a partida confirmando seu e-mail com o código abaixo.
                                    </p>
                                    
                                    <div class="code-container">
                                        <p class="code">{{CODE}}</p>
                                    </div>
                                    
                                    <p class="text" style="margin-bottom: 0;">
                                        Sua chave de ignição é válida por <strong>10 minutos</strong>.
                                    </p>
                                    <p class="warning-text" style="margin-top: 12px;">
                                        Se você não estacionou no CarsHub, por favor ignore esta mensagem.
                                    </p>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="footer">
                                <p class="footer-text">&copy; 2026 CarsHub. O clássico nunca morre.</p>
                                <p class="footer-text">
                                    <a href="#" class="footer-link">Manual do Proprietário (Termos)</a> • <a href="#" class="footer-link">Privacidade</a>
                                </p>
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
            """;

            String finalHtml = htmlBody.replace("{{CODE}}", code);

            helper.setText(finalHtml, true);
            helper.setTo(to);
            helper.setSubject("🚘 " + code + " é o seu código de ignição - CarsHub");
            helper.setFrom("CarsHub <noreply@carshub.com.br>");

            mailSender.send(mimeMessage);

        } catch (Exception error) {
            throw new RuntimeException("Erro ao enviar e-mail HTML: " + error.getMessage());
        }
    }

    public void sendTwoFactorAuthCode(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Template HTML - Verificação de 2 Etapas (2FA) Tema Carros
            String htmlBody = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chave de Acesso da Garagem</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;1,600&family=Lora:wght@400;500&display=swap');
                    
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #121212; 
                        font-family: 'Lora', serif;
                        color: #e6e6e6; 
                        -webkit-font-smoothing: antialiased;
                    }
                    .wrapper {
                        width: 100%;
                        table-layout: fixed;
                        background-color: #121212;
                        padding-bottom: 60px;
                    }
                    .main-table {
                        background-color: #121212;
                        margin: 0 auto;
                        width: 100%;
                        max-width: 600px;
                        border-spacing: 0;
                        color: #e6e6e6;
                    }
                    .logo-container {
                        padding: 40px 0 20px;
                        text-align: center;
                    }
                    .logo {
                        font-family: 'Playfair Display', serif;
                        font-size: 32px;
                        font-weight: 700;
                        color: #ffffff;
                        text-decoration: none;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .logo span {
                        color: #d4af37;
                    }
                    .card {
                        background-color: #1c1c1c;
                        border: 1px solid #333333;
                        border-top: 4px solid #d4af37;
                        border-radius: 8px;
                        padding: 48px 40px;
                        text-align: center;
                        box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.8);
                    }
                    .icon-container {
                        margin-bottom: 24px;
                    }
                    .shield-icon {
                        display: inline-block;
                        width: 56px;
                        height: 56px;
                        background-color: rgba(212, 175, 55, 0.1);
                        border: 1px solid rgba(212, 175, 55, 0.3);
                        border-radius: 50%;
                        line-height: 54px;
                        color: #d4af37;
                        font-size: 28px;
                    }
                    .title {
                        font-family: 'Playfair Display', serif;
                        color: #ffffff;
                        font-size: 26px;
                        font-weight: 700;
                        margin: 0 0 16px;
                        letter-spacing: 0.5px;
                    }
                    .text {
                        color: #b3b3b3; 
                        font-size: 16px;
                        line-height: 1.7;
                        margin: 0 0 32px;
                    }
                    .code-container {
                        background: linear-gradient(145deg, #161616, #222222);
                        border: 1px solid #d4af37; 
                        border-radius: 6px;
                        padding: 24px;
                        margin: 0 0 32px;
                        box-shadow: inset 0 2px 10px rgba(0,0,0,0.5);
                    }
                    .code {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 34px;
                        font-weight: 700;
                        color: #d4af37;
                        letter-spacing: 12px;
                        margin: 0;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.5);
                    }
                    .warning-text {
                        font-size: 13px;
                        color: #666666; 
                        margin: 0;
                        font-family: sans-serif;
                    }
                    .footer {
                        padding-top: 32px;
                        text-align: center;
                        font-family: sans-serif;
                    }
                    .footer-text {
                        color: #777777; 
                        font-size: 12px;
                        margin: 4px 0;
                    }
                    .footer-link {
                        color: #d4af37;
                        text-decoration: none;
                    }
                    
                    @media screen and (max-width: 600px) {
                        .card {
                            padding: 32px 20px;
                            border-radius: 0;
                            border-left: none;
                            border-right: none;
                        }
                        .code {
                            font-size: 28px;
                            letter-spacing: 6px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <table class="main-table" role="presentation">
                        <tr>
                            <td class="logo-container">
                                <a href="#" class="logo">Cars<span>Hub</span></a>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <div class="card">         
                                    <div class="icon-container">
                                        <!-- Ícone de chave antiga -->
                                        <span class="shield-icon">&#128477;</span>
                                    </div>
                                    
                                    <h1 class="title">Acesso Restrito (2FA)</h1>
                                    
                                    <p class="text">
                                        Ouvimos os motores ligarem. Para garantir que é realmente você atrás do volante, utilize a chave de verificação abaixo para acessar sua conta.
                                    </p>
                                    
                                    <div class="code-container">
                                        <p class="code">{{CODE}}</p>
                                    </div>
                                    
                                    <p class="text" style="margin-bottom: 0;">
                                        Este código é válido por <strong>5 minutos</strong>.
                                    </p>
                                    <p class="warning-text" style="margin-top: 12px;">
                                        Se você não tentou fazer o login, puxe o freio de mão e altere sua senha imediatamente.
                                    </p>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="footer">
                                <p class="footer-text">&copy; 2026 CarsHub. Todos os direitos reservados.</p>
                                <p class="footer-text">
                                    <a href="#" class="footer-link">Manual do Proprietário</a> • <a href="#" class="footer-link">Privacidade</a>
                                </p>
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
            """;

            String finalHtml = htmlBody.replace("{{CODE}}", code);

            helper.setText(finalHtml, true);
            helper.setTo(to);
            helper.setSubject("🗝️ " + code + " é sua chave de acesso - CarsHub");
            helper.setFrom("CarsHub <noreply@carshub.com.br>");

            mailSender.send(mimeMessage);

        } catch (Exception error) {
            throw new RuntimeException("Erro ao enviar e-mail de 2FA: " + error.getMessage());
        }
    }

    public void sendEnableTwoFactorCode(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Template HTML - Habilitação de 2FA Tema Carros
            String htmlBody = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Blindagem da Frota</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;1,600&family=Lora:wght@400;500&display=swap');
                    
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #121212; 
                        font-family: 'Lora', serif;
                        color: #e6e6e6; 
                        -webkit-font-smoothing: antialiased;
                    }
                    .wrapper {
                        width: 100%;
                        table-layout: fixed;
                        background-color: #121212;
                        padding-bottom: 60px;
                    }
                    .main-table {
                        background-color: #121212;
                        margin: 0 auto;
                        width: 100%;
                        max-width: 600px;
                        border-spacing: 0;
                        color: #e6e6e6;
                    }
                    .logo-container {
                        padding: 40px 0 20px;
                        text-align: center;
                    }
                    .logo {
                        font-family: 'Playfair Display', serif;
                        font-size: 32px;
                        font-weight: 700;
                        color: #ffffff;
                        text-decoration: none;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .logo span {
                        color: #d4af37;
                    }
                    .card {
                        background-color: #1c1c1c;
                        border: 1px solid #333333;
                        border-top: 4px solid #d4af37;
                        border-radius: 8px;
                        padding: 48px 40px;
                        text-align: center;
                        box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.8);
                    }
                    .icon-container {
                        margin-bottom: 24px;
                    }
                    .shield-icon {
                        display: inline-block;
                        width: 56px;
                        height: 56px;
                        background-color: rgba(212, 175, 55, 0.1);
                        border: 1px solid rgba(212, 175, 55, 0.3);
                        border-radius: 50%;
                        line-height: 54px;
                        color: #d4af37;
                        font-size: 28px;
                    }
                    .title {
                        font-family: 'Playfair Display', serif;
                        color: #ffffff;
                        font-size: 26px;
                        font-weight: 700;
                        margin: 0 0 16px;
                        letter-spacing: 0.5px;
                    }
                    .text {
                        color: #b3b3b3; 
                        font-size: 16px;
                        line-height: 1.7;
                        margin: 0 0 32px;
                    }
                    .code-container {
                        background: linear-gradient(145deg, #161616, #222222);
                        border: 1px solid #d4af37; 
                        border-radius: 6px;
                        padding: 24px;
                        margin: 0 0 32px;
                        box-shadow: inset 0 2px 10px rgba(0,0,0,0.5);
                    }
                    .code {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 34px;
                        font-weight: 700;
                        color: #d4af37;
                        letter-spacing: 12px;
                        margin: 0;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.5);
                    }
                    .warning-text {
                        font-size: 13px;
                        color: #666666; 
                        margin: 0;
                        font-family: sans-serif;
                    }
                    .footer {
                        padding-top: 32px;
                        text-align: center;
                        font-family: sans-serif;
                    }
                    .footer-text {
                        color: #777777; 
                        font-size: 12px;
                        margin: 4px 0;
                    }
                    .footer-link {
                        color: #d4af37;
                        text-decoration: none;
                    }
                    
                    @media screen and (max-width: 600px) {
                        .card {
                            padding: 32px 20px;
                            border-radius: 0;
                            border-left: none;
                            border-right: none;
                        }
                        .code {
                            font-size: 28px;
                            letter-spacing: 6px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <table class="main-table" role="presentation">
                        <tr>
                            <td class="logo-container">
                                <a href="#" class="logo">Cars<span>Hub</span></a>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <div class="card">         
                                    <div class="icon-container">
                                        <!-- Ícone de ferramentas -->
                                        <span class="shield-icon">&#128736;</span>
                                    </div>
                                    
                                    <h1 class="title">Blindagem Engatada</h1>
                                    
                                    <p class="text">
                                        Você solicitou um reforço extra de segurança na sua conta (2FA). Utilize o código abaixo para finalizar essa manutenção e manter sua garagem protegida.
                                    </p>
                                    
                                    <div class="code-container">
                                        <p class="code">{{CODE}}</p>
                                    </div>
                                    
                                    <p class="text" style="margin-bottom: 0;">
                                        Este código é válido por <strong>10 minutos</strong>.
                                    </p>
                                    <p class="warning-text" style="margin-top: 12px;">
                                        Se você não solicitou essa blindagem extra, por favor ignore este e-mail.
                                    </p>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="footer">
                                <p class="footer-text">&copy; 2026 CarsHub. Todos os direitos reservados.</p>
                                <p class="footer-text">
                                    <a href="#" class="footer-link">Manual do Proprietário</a> • <a href="#" class="footer-link">Privacidade</a>
                                </p>
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
            """;

            String finalHtml = htmlBody.replace("{{CODE}}", code);

            helper.setText(finalHtml, true);
            helper.setTo(to);
            helper.setSubject("🛠️ " + code + " é o seu código para ativar 2FA - CarsHub");
            helper.setFrom("CarsHub <noreply@carshub.com.br>");

            mailSender.send(mimeMessage);

        } catch (Exception error) {
            throw new RuntimeException("Erro ao enviar e-mail de habilitação de 2FA: " + error.getMessage());
        }
    }

}