package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.TipoServico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Gera a chave de idempotencia das solicitacoes.
 *
 * A chave e o SHA-256 de: colecionador | carro | tipo de servico | servicos
 * pedidos em ordem alfabetica. Isso produz EXATAMENTE o comportamento pedido:
 *
 *   - dois colecionadores diferentes -> chaves diferentes (pode repetir);
 *   - o mesmo colecionador, carros diferentes -> chaves diferentes (pode repetir);
 *   - o mesmo colecionador + mesmo carro + mesma lista -> MESMA chave (bloqueia);
 *   - depois de FINALIZADO a chave e zerada, entao a mesma solicitacao pode ser
 *     criada de novo no futuro (e o exemplo 3 da decisao).
 *
 * O Id fica de fora da comparacao, como combinado (ele nunca repete).
 */
public final class GeradorChaveIdempotencia {

    private GeradorChaveIdempotencia() {
    }

    public static String gerar(UUID idEntusiasta, UUID idCarro, TipoServico tipoServico,
                              List<ServicosOfertados> servicos) {
        StringBuilder base = new StringBuilder()
                .append(idEntusiasta).append('|')
                .append(idCarro).append('|')
                .append(tipoServico.name()).append('|');

        if (servicos != null) {
            servicos.stream()
                    .map(Enum::name)
                    .sorted()
                    .forEach(servico -> base.append(servico).append(','));
        }

        return hashSha256(base.toString());
    }

    private static String hashSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexadecimal = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hexadecimal.append(String.format("%02x", b));
            }

            return hexadecimal.toString();
        }
        catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("O algoritmo SHA-256 não está disponível nesta JVM.", erro);
        }
    }
}