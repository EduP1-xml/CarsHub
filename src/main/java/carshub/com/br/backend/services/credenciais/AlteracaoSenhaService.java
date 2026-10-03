package carshub.com.br.backend.services.credenciais;

import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Centraliza os dois fluxos de troca de senha:
 *
 *   LOGADO    - solicitado pelo perfil do usuario; a identidade vem da sessao.
 *               Cooldown de 60 segundos entre solicitacoes.
 *
 *   DESLOGADO - "esqueci minha senha"; a identidade vem do e-mail informado.
 *               Cooldown de 24 horas entre solicitacoes, para dificultar que
 *               terceiros usem a rota como vetor de spam ou de tomada de conta.
 *
 * Em ambos os fluxos o codigo enviado por e-mail precisa ser confirmado antes de
 * a nova senha ser aceita, e codigo expirado nunca e aceito: o usuario precisa
 * solicitar um novo.
 *
 * REGRA DE TRANSACAO: o envio de e-mail ocorre sempre FORA da transacao, apos o
 * commit, para que o I/O de rede nao segure conexao do banco nem desfaca dados
 * de negocio quando o servidor de e-mail falhar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlteracaoSenhaService {

    /** Intervalo minimo entre solicitacoes no fluxo logado. */
    private static final long intervaloMinimoSolicitacaoSegundos = 60;

    /** Intervalo minimo entre solicitacoes no fluxo deslogado. */
    private static final long intervaloMinimoRedefinicaoHoras = 24;

    private static final long segundosPorHora = 3600;

    private final UsuarioRepository repositorioUsuario;
    private final CredencialService servicoCredencial;
    private final CodigoVerificacaoService servicoCodigoVerificacao;
    private final EmailService servicoEmail;
    private final TransactionTemplate templateTransacao;

    // ==================================================================
    // FLUXO LOGADO
    // ==================================================================

    public void solicitarCodigoAlteracao(Usuario usuario) {
        String codigo = templateTransacao.execute(
                status -> gerarCodigoAlteracaoSenha(usuario.getId()));

        if (codigo == null) {
            throw new RegraDeNegocioException("Não foi possível gerar um novo código. Tente novamente.");
        }

        // Fora da transacao. Falha propaga (502/503/504): o objetivo e entregar o codigo.
        servicoEmail.enviarCodigoRedefinicaoSenha(usuario.getLogin(), codigo);
    }

    @Transactional
    public void alterarSenha(Usuario usuario, AlteracaoSenhaDTO dados) {
        servicoCredencial.validarPolitica(dados.novaSenha());
        servicoCodigoVerificacao.validar(
                usuario.getCodigoRedefinicaoSenha(),
                usuario.getExpiracaoCodigoRedefinicaoSenha(),
                dados.codigo());

        aplicarNovaSenha(usuario, dados.novaSenha());
    }

    // ==================================================================
    // FLUXO DESLOGADO
    // ==================================================================

    /**
     * A resposta e sempre a mesma, exista ou nao o e-mail, para nao permitir que
     * alguem descubra quais contas existem na plataforma.
     *
     * Pelo mesmo motivo, o cooldown aqui e SILENCIOSO: quando o intervalo minimo
     * ainda nao passou, nenhum e-mail e enviado e a resposta continua identica.
     * Se respondessemos um erro de cooldown, um atacante poderia descobrir que o
     * e-mail existe simplesmente comparando as respostas.
     */
    public void solicitarCodigoRedefinicao(String email) {
        String codigo = templateTransacao.execute(status ->
                repositorioUsuario.findByLogin(email)
                        .map(usuario -> gerarCodigoRedefinicaoSenha(usuario.getId()))
                        .orElse(null));

        // E-mail inexistente OU cooldown de 24h ativo: encerra silenciosamente.
        if (codigo == null) {
            return;
        }

        servicoEmail.enviarCodigoRedefinicaoSenha(email, codigo);
    }

    @Transactional
    public void redefinirSenha(RedefinicaoSenhaDTO dados) {
        Usuario usuario = repositorioUsuario.findByLogin(dados.email())
                .orElseThrow(() -> new RegraDeNegocioException("Código de verificação inválido."));

        servicoCredencial.validarPolitica(dados.novaSenha());
        servicoCodigoVerificacao.validar(
                usuario.getCodigoRedefinicaoSenha(),
                usuario.getExpiracaoCodigoRedefinicaoSenha(),
                dados.codigo());

        aplicarNovaSenha(usuario, dados.novaSenha());
    }

    // ==================================================================
    // GERACAO DE CODIGO (executada dentro da transacao)
    // ==================================================================

    private String gerarCodigoAlteracaoSenha(UUID idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);

        aplicarCooldown(
                usuario.getDataEnvioCodigoAlteracaoSenha(),
                intervaloMinimoSolicitacaoSegundos);

        usuario.setDataEnvioCodigoAlteracaoSenha(LocalDateTime.now());
        return aplicarNovoCodigo(usuario);
    }

    /**
     * Retorna null quando o cooldown de 24 horas esta ativo. O chamador trata o
     * null como "nao enviar nada", preservando a resposta generica.
     */
    private String gerarCodigoRedefinicaoSenha(UUID idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);

        LocalDateTime ultimoEnvio = usuario.getDataEnvioCodigoRedefinicaoSenha();
        if (cooldownAtivo(ultimoEnvio, intervaloMinimoRedefinicaoHoras * segundosPorHora)) {
            log.warn("Solicitação de redefinição de senha ignorada por cooldown de {} horas: {}",
                    intervaloMinimoRedefinicaoHoras, usuario.getLogin());
            return null;
        }

        usuario.setDataEnvioCodigoRedefinicaoSenha(LocalDateTime.now());
        return aplicarNovoCodigo(usuario);
    }

    private String aplicarNovoCodigo(Usuario usuario) {
        // O codigo novo SUBSTITUI o anterior, que deixa de funcionar,
        // e reinicia o prazo de 10 minutos.
        String codigo = servicoCodigoVerificacao.gerarCodigo();
        usuario.setCodigoRedefinicaoSenha(codigo);
        usuario.setExpiracaoCodigoRedefinicaoSenha(servicoCodigoVerificacao.calcularExpiracao());
        repositorioUsuario.save(usuario);

        return codigo;
    }

    // ==================================================================
    // METODOS AUXILIARES
    // ==================================================================

    private Usuario buscarUsuario(UUID idUsuario) {
        return repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));
    }

    private void aplicarCooldown(LocalDateTime ultimoEnvio, long intervaloSegundos) {
        if (!cooldownAtivo(ultimoEnvio, intervaloSegundos)) {
            return;
        }

        LocalDateTime liberadoEm = ultimoEnvio.plusSeconds(intervaloSegundos);
        long segundosRestantes = Duration.between(LocalDateTime.now(), liberadoEm).toSeconds() + 1;

        throw new RegraDeNegocioException(
                "Aguarde " + formatarTempoDeEspera(segundosRestantes) + " para solicitar um novo código.");
    }

    private boolean cooldownAtivo(LocalDateTime ultimoEnvio, long intervaloSegundos) {
        if (ultimoEnvio == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(ultimoEnvio.plusSeconds(intervaloSegundos));
    }

    private String formatarTempoDeEspera(long segundos) {
        if (segundos >= segundosPorHora) {
            long horas = (segundos + segundosPorHora - 1) / segundosPorHora;
            return horas + (horas == 1 ? " hora" : " horas");
        }
        if (segundos >= 60) {
            long minutos = (segundos + 59) / 60;
            return minutos + (minutos == 1 ? " minuto" : " minutos");
        }
        return segundos + (segundos == 1 ? " segundo" : " segundos");
    }

    private void aplicarNovaSenha(Usuario usuario, String novaSenha) {
        servicoCredencial.atualizarSenha(usuario, novaSenha);

        // O codigo e consumido: nao pode ser reutilizado.
        usuario.setCodigoRedefinicaoSenha(null);
        usuario.setExpiracaoCodigoRedefinicaoSenha(null);
        usuario.setDataEnvioCodigoAlteracaoSenha(null);
        usuario.setDataEnvioCodigoRedefinicaoSenha(null);

        repositorioUsuario.save(usuario);
    }
}