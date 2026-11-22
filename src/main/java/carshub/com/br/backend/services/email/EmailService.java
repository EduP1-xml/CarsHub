package carshub.com.br.backend.services.email;

import carshub.com.br.backend.exceptions.AutenticacaoEnvioEmailException;
import carshub.com.br.backend.exceptions.FalhaEnvioEmailException;
import carshub.com.br.backend.exceptions.TempoLimiteEnvioEmailException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Service
public class EmailService {

    private final JavaMailSender remetenteEmail;

    /**
     * Endereco da conta autenticada no SMTP. O Gmail recusa o envio quando o
     * cabecalho "From" aponta para um endereco diferente da conta autenticada,
     * por isso ele e lido da mesma propriedade usada na autenticacao.
     */
    @Value("${spring.mail.username}")
    private String enderecoRemetente;

    /**
     * Template base unificado - AJUSTE FINAL (anti-spam):
     *   - sem emojis no assunto nem no corpo;
     *   - o codigo de verificacao NAO aparece no assunto (privacidade + filtros);
     *   - sem @import de fontes externas (Gmail ignora e pontua como suspeito);
     *   - HTML claro e enxuto, com estilos inline (compativel com Gmail/Outlook);
     *   - sem links falsos (href="#") nem termos de urgencia.
     * Os simbolos '%' do CSS sao escapados com '%%' para uso com o String.format.
     */
    private static final String templateHtmlBase = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;color:#333333;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:24px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:100%%;max-width:600px;background-color:#ffffff;border:1px solid #dddddd;border-radius:6px;">
                                <tr>
                                    <td style="padding:32px 40px;">
                                        <p style="margin:0 0 24px;font-size:20px;font-weight:bold;color:#1a1a1a;">CarsHub</p>
                                        <h1 style="margin:0 0 16px;font-size:22px;font-weight:bold;color:#1a1a1a;">%s</h1>
                                        <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#444444;">%s</p>
                                        <p style="margin:0 0 24px;font-size:28px;font-weight:bold;letter-spacing:4px;color:#8a6d1a;font-family:'Courier New',Courier,monospace;">%s</p>
                                        <p style="margin:0 0 8px;font-size:13px;color:#666666;">%s</p>
                                        <p style="margin:0;font-size:12px;color:#999999;">%s</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

    public EmailService(JavaMailSender remetenteEmail) {
        this.remetenteEmail = remetenteEmail;
    }

    public JavaMailSender obterRemetenteEmail() {
        return remetenteEmail;
    }

    /**
     * Metodo central que insere os textos dinamicamente no template base.
     *
     * Toda falha de SMTP e traduzida para uma excecao de dominio especifica:
     *   - credencial recusada  -> AutenticacaoEnvioEmailException (HTTP 502)
     *   - tempo limite excedido -> TempoLimiteEnvioEmailException  (HTTP 504)
     *   - qualquer outro erro   -> FalhaEnvioEmailException        (HTTP 503)
     */
    private void enviarEmailGenerico(String destinatario, String assunto, String tituloPagina,
                                     String tituloPrincipal, String textoCorpo,
                                     String codigo, String textoExpiracao, String textoAviso) {
        try {
            MimeMessage mensagemMime = remetenteEmail.createMimeMessage();
            MimeMessageHelper auxiliar = new MimeMessageHelper(mensagemMime, "utf-8");

            String htmlFinal = String.format(templateHtmlBase,
                    tituloPagina, tituloPrincipal, textoCorpo, codigo, textoExpiracao, textoAviso);

            auxiliar.setText(htmlFinal, true);
            auxiliar.setTo(destinatario);
            auxiliar.setSubject(assunto);
            auxiliar.setFrom("CarsHub <" + enderecoRemetente + ">");

            remetenteEmail.send(mensagemMime);

        } catch (Exception erro) {
            throw traduzirFalhaDeEnvio(destinatario, erro);
        }
    }

    private FalhaEnvioEmailException traduzirFalhaDeEnvio(String destinatario, Exception erro) {
        if (ocorreuAutenticacaoRecusada(erro)) {
            return new AutenticacaoEnvioEmailException(
                    "O servidor de e-mail recusou a autenticação. "
                            + "Verifique o usuário e a senha de app configurados no servidor.", erro);
        }

        if (ocorreuTempoLimite(erro)) {
            return new TempoLimiteEnvioEmailException(
                    "O servidor de e-mail não respondeu dentro do tempo limite configurado.", erro);
        }

        return new FalhaEnvioEmailException(
                "Falha ao enviar e-mail para " + destinatario + ": " + erro.getMessage(), erro);
    }

    /**
     * Autenticacao recusada vem como MailAuthenticationException (Spring) ou
     * AuthenticationFailedException (Jakarta Mail) dentro da cadeia de causas.
     */
    private boolean ocorreuAutenticacaoRecusada(Throwable erro) {
        return contemCausa(erro, MailAuthenticationException.class, AuthenticationFailedException.class);
    }

    /**
     * Timeout vem como SocketTimeoutException. Isso cobre os tres timeouts
     * configurados: connectiontimeout, timeout (leitura) e writetimeout.
     *
     * Nao existe classe dedicada a falha de conexao no Spring 6+:
     * MailConnectException foi removida e esses erros chegam como MailSendException.
     */
    private boolean ocorreuTempoLimite(Throwable erro) {
        return contemCausa(erro, SocketTimeoutException.class);
    }

    /**
     * Percorre toda a arvore de causas do erro, cobrindo os tres caminhos pelos
     * quais o Spring Mail e o Jakarta Mail encadeiam a falha original:
     * getCause(), MessagingException.getNextException() e o mapa de mensagens
     * que falharam em MailSendException.
     */
    private boolean contemCausa(Throwable erro, Class<?>... tipos) {
        Set<Throwable> visitados = new HashSet<>();
        Deque<Throwable> pendentes = new ArrayDeque<>();
        pendentes.add(erro);

        while (!pendentes.isEmpty()) {
            Throwable atual = pendentes.poll();
            if (atual == null || !visitados.add(atual)) {
                continue;
            }

            for (Class<?> tipo : tipos) {
                if (tipo.isInstance(atual)) {
                    return true;
                }
            }

            if (atual instanceof MailSendException falhaDeEnvio) {
                pendentes.addAll(falhaDeEnvio.getFailedMessages().values());
            }
            if (atual instanceof MessagingException mensagem) {
                pendentes.add(mensagem.getNextException());
            }
            pendentes.add(atual.getCause());
        }

        return false;
    }

    // ==================================================================
    // 1. Criacao de conta (primeira verificacao)
    // ==================================================================

    public void enviarCodigoVerificacao(String destinatario, String codigo) {
        enviarEmailGenerico(
                destinatario,
                "CarsHub - Código de verificação de e-mail",
                "Verificação de e-mail - CarsHub",
                "Confirme seu e-mail",
                "Recebemos um pedido de cadastro na CarsHub. Para concluir, informe o código abaixo na tela de cadastro.",
                codigo,
                "Este código é válido por 10 minutos.",
                "Se você não fez este cadastro, ignore esta mensagem."
        );
    }

    // ==================================================================
    // 2. Recuperacao e alteracao de senha
    // ==================================================================

    public void enviarCodigoRedefinicaoSenha(String destinatario, String codigo) {
        enviarEmailGenerico(
                destinatario,
                "CarsHub - Recuperação de senha",
                "Recuperação de senha - CarsHub",
                "Recuperação de senha",
                "Recebemos uma solicitação para redefinir a senha da sua conta. Use o código abaixo para criar uma nova senha.",
                codigo,
                "Este código expira em 10 minutos.",
                "Se você não solicitou a redefinição, ignore esta mensagem. Sua senha permanece inalterada."
        );
    }

    // ==================================================================
    // 3. Login com 2FA
    // ==================================================================

    public void enviarCodigoDuploFator(String destinatario, String codigo) {
        enviarEmailGenerico(
                destinatario,
                "CarsHub - Código de acesso (verificação em duas etapas)",
                "Acesso em duas etapas - CarsHub",
                "Código de acesso",
                "Use o código abaixo para confirmar sua identidade e acessar sua conta.",
                codigo,
                "Este código é válido por 5 minutos.",
                "Se você não tentou entrar na sua conta, altere sua senha imediatamente."
        );
    }

    // ==================================================================
    // 4. Habilitacao do 2FA
    // ==================================================================

    public void enviarCodigoAtivacaoDuploFator(String destinatario, String codigo) {
        enviarEmailGenerico(
                destinatario,
                "CarsHub - Ativação da verificação em duas etapas",
                "Verificação em duas etapas - CarsHub",
                "Ativação da verificação em duas etapas",
                "Você solicitou a ativação da verificação em duas etapas na sua conta. Use o código abaixo para concluir a ativação.",
                codigo,
                "Este código é válido por 10 minutos.",
                "Se você não solicitou esta ativação, ignore esta mensagem."
        );
    }
}
