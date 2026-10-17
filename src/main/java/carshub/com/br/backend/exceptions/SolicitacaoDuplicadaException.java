package carshub.com.br.backend.exceptions;

/**
 * Ja existe uma solicitacao ATIVA identica (mesmo colecionador, mesmo carro,
 * mesmo tipo e mesmos servicos). Mapeada para HTTP 409, como as demais
 * duplicidades do projeto (e-mail, CPF/CNPJ, placa).
 */
public class SolicitacaoDuplicadaException extends RuntimeException {

    public SolicitacaoDuplicadaException(String mensagem) {
        super(mensagem);
    }
}