package carshub.com.br.backend.services.credenciais;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class CodigoVerificacaoService {

    private static final int validadeMinutos = 10;

    private final SecureRandom geradorAleatorio = new SecureRandom();

    /**
     * Gera um codigo numerico de 6 digitos, sempre com zeros a esquerda.
     */
    public String gerarCodigo() {
        return String.format("%06d", geradorAleatorio.nextInt(1_000_000));
    }

    public LocalDateTime calcularExpiracao() {
        return LocalDateTime.now().plusMinutes(validadeMinutos);
    }


    public void validar(String codigoArmazenado, LocalDateTime expiracaoArmazenada, String codigoInformado) {
        if (codigoInformado == null || codigoInformado.isBlank()) {
            throw new CampoObrigatorioException("O código de verificação é obrigatório.");
        }

        if (codigoArmazenado == null || !codigoArmazenado.equals(codigoInformado)) {
            throw new RegraDeNegocioException("Código de verificação inválido.");
        }

        if (expiracaoArmazenada == null || expiracaoArmazenada.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Código de verificação expirado. Solicite um novo.");
        }
    }
}