package carshub.com.br.backend.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento global de excecoes da API.
 *
 * REGRA: erro causado pelo cliente (payload, header, Content-Type, rota) retorna
 * 4xx com mensagem util; falha de servico externo retorna 502/503/504; somente
 * falha inesperada da propria aplicacao retorna 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalException {

    // ==================================================================
    // 400 - REQUISICAO MALFORMADA
    // ==================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacoesException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> corpoNaoLegivel(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("O corpo da requisição está ausente ou mal formatado. Verifique o JSON enviado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("O parâmetro '" + ex.getName() + "' recebeu um valor inválido: " + ex.getValue());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> headerObrigatorioAusente(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("O cabeçalho obrigatório '" + ex.getHeaderName() + "' não foi informado.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> parteObrigatoriaAusente(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("A parte obrigatória '" + ex.getRequestPartName() + "' não foi enviada no formulário.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> violacaoDeRestricao(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Falha de validacao do Bean Validation disparada pelo Hibernate ao persistir.
     * O Spring embrulha a ConstraintViolationException em TransactionSystemException.
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<String> falhaDeValidacaoNaTransacao(TransactionSystemException ex) {
        Throwable causa = ex.getMostSpecificCause();

        if (causa instanceof ConstraintViolationException violacao) {
            String mensagens = violacao.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));

            log.warn("Falha de validação ao persistir a entidade: {}", mensagens);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagens);
        }

        log.error("Falha inesperada ao confirmar a transação: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    // ==================================================================
    // 405 - METODO HTTP ERRADO
    // ==================================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("O método HTTP '" + ex.getMethod() + "' não é suportado nesta rota.");
    }

    // ==================================================================
    // 413 - ARQUIVO GRANDE DEMAIS
    // ==================================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> arquivoMuitoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("O arquivo enviado excede o tamanho máximo permitido (15MB).");
    }

    // ==================================================================
    // 415 - CONTENT-TYPE NAO SUPORTADO
    // ==================================================================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> tipoDeConteudoNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("O Content-Type '" + ex.getContentType() + "' não é suportado nesta rota. "
                        + "Envie o corpo como application/json (aba raw no Postman).");
    }

    // ==================================================================
    // 403 - ACESSO NEGADO
    // ==================================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> acessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Acesso negado: você não tem permissão para executar esta operação.");
    }

    // ==================================================================
    // 404 - RECURSO NAO ENCONTRADO
    // ==================================================================

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<String> recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // ==================================================================
    // 401 - NAO AUTENTICADO
    // ==================================================================

    @ExceptionHandler(SenhaIncorretaException.class)
    public ResponseEntity<String> senhaIncorreta(SenhaIncorretaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    // ==================================================================
    // 409 - CONFLITO DE DADOS
    // ==================================================================

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<String> emailJaCadastrado(EmailJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(DocumentoJaCadastradoException.class)
    public ResponseEntity<String> documentoJaCadastrado(DocumentoJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(PlacaJaCadastradaException.class)
    public ResponseEntity<String> placaJaCadastrada(PlacaJaCadastradaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> violacaoDeIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco de dados: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Os dados informados conflitam com um registro já existente.");
    }

    // ==================================================================
    // 400 - CAMPO OBRIGATORIO
    // ==================================================================

    @ExceptionHandler(CampoObrigatorioException.class)
    public ResponseEntity<String> campoObrigatorio(CampoObrigatorioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // ==================================================================
    // 422 - REGRA DE NEGOCIO
    // ==================================================================

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<String> regraDeNegocio(RegraDeNegocioException ex){
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ex.getMessage());
    }

    @ExceptionHandler(PerfilIncompativelException.class)
    public ResponseEntity<String> perfilIncompativel(PerfilIncompativelException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // ==================================================================
    // 502 - AUTENTICACAO RECUSADA PELO SERVIDOR DE E-MAIL
    // ==================================================================

    /**
     * O servidor de e-mail respondeu, mas recusou a credencial apresentada.
     * Indica erro de configuracao do servidor (usuario/senha de app invalidos),
     * nao erro da requisicao do cliente.
     */
    @ExceptionHandler(AutenticacaoEnvioEmailException.class)
    public ResponseEntity<String> autenticacaoEnvioEmail(AutenticacaoEnvioEmailException ex) {
        log.error("Autenticação recusada pelo servidor de e-mail: ", ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("O serviço de e-mail recusou a autenticação. "
                        + "A configuração de envio precisa ser corrigida pelo administrador.");
    }

    // ==================================================================
    // 504 - TEMPO LIMITE DO SERVIDOR DE E-MAIL
    // ==================================================================

    /**
     * O servidor de e-mail foi alcancado, mas nao respondeu dentro do tempo
     * limite configurado.
     */
    @ExceptionHandler(TempoLimiteEnvioEmailException.class)
    public ResponseEntity<String> tempoLimiteEnvioEmail(TempoLimiteEnvioEmailException ex) {
        log.error("Tempo limite excedido no envio de e-mail: ", ex);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body("O serviço de e-mail demorou demais para responder. "
                        + "Tente novamente em alguns instantes.");
    }

    // ==================================================================
    // 503 - FALHA GENERICA NO ENVIO DE E-MAIL
    // ==================================================================

    @ExceptionHandler(FalhaEnvioEmailException.class)
    public ResponseEntity<String> falhaEnvioEmail(FalhaEnvioEmailException ex) {
        log.error("Falha no envio de e-mail: ", ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Não foi possível enviar o e-mail no momento. "
                        + "Aguarde alguns instantes e solicite um novo código.");
    }

    // ==================================================================
    // 502 - CHAVE DA API DE GEOCODIFICACAO RECUSADA OU AUSENTE
    // ==================================================================

    /**
     * O servico de mapas respondeu, mas recusou a chave apresentada (ou ela nem
     * foi configurada). E erro de configuracao do servidor, nao do cliente.
     */
    @ExceptionHandler(ChaveGeocodificacaoException.class)
    public ResponseEntity<String> chaveGeocodificacao(ChaveGeocodificacaoException ex) {
        log.error("Chave da API de geocodificação recusada ou ausente: ", ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("O serviço de mapas recusou a chave de API. "
                        + "A configuração precisa ser corrigida pelo administrador.");
    }

    // ==================================================================
    // 504 - TEMPO LIMITE DA API DE GEOCODIFICACAO
    // ==================================================================

    @ExceptionHandler(TempoLimiteGeocodificacaoException.class)
    public ResponseEntity<String> tempoLimiteGeocodificacao(TempoLimiteGeocodificacaoException ex) {
        log.error("Tempo limite excedido na geocodificação: ", ex);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body("O serviço de mapas demorou demais para responder. "
                        + "Tente novamente ou envie latitude e longitude do GPS.");
    }

    // ==================================================================
    // 503 - FALHA GENERICA NA API DE GEOCODIFICACAO
    // ==================================================================

    @ExceptionHandler(FalhaGeocodificacaoException.class)
    public ResponseEntity<String> falhaGeocodificacao(FalhaGeocodificacaoException ex) {
        log.error("Falha na geocodificação: ", ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Não foi possível localizar o endereço no momento. "
                        + "Tente novamente ou envie latitude e longitude do GPS.");
    }

    // ==================================================================
    // 500 - FALHA INESPERADA DO SERVIDOR
    // ==================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> errosInesperados(Exception ex) {
        log.error("Erro inesperado no servidor: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ocorreu um erro interno inesperado no servidor. Por favor, tente novamente mais tarde.");
    }
}