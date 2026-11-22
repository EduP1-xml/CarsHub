package carshub.com.br.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SPRINT 6 (11.9): trilha de auditoria de acessos e acoes sensiveis
 * (login/renovacao de token/troca de senha/aceite de proposta/conclusao/
 * cancelamento). Tabela criada pelo ddl-auto: update como as demais.
 *
 * Nunca grava senha nem token: apenas acao, recurso, detalhes genericos, id do
 * ator e IP de origem. Registro imutavel — sem endpoint de edicao/exclusao.
 */
@Entity
@Table(name = "Auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "id_Auditoria", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /** Acao sensivel executada (ex.: LOGIN_SUCESSO, PROPOSTA_ACEITA). */
    @Column(name = "acao_Auditoria", length = 60, nullable = false)
    private String acao;

    /** Recurso alvo (ex.: login do usuario, id do chamado). */
    @Column(name = "recurso_Auditoria", length = 120)
    private String recurso;

    /** Complemento generico (nunca senha/token). */
    @Column(name = "detalhes_Auditoria", length = 255)
    private String detalhes;

    @Column(name = "idUsuario_Auditoria", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID idUsuario;

    @Column(name = "enderecoIp_Auditoria", length = 45)
    private String enderecoIp;

    @CreationTimestamp
    @Column(name = "dataHora_Auditoria", nullable = false, updatable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHora;

    @Column(name = "sucesso_Auditoria", nullable = false)
    private Boolean sucesso;

    public static Auditoria novo(String acao, String recurso, String detalhes,
                                 UUID idUsuario, String enderecoIp, boolean sucesso) {
        Auditoria auditoria = new Auditoria();
        auditoria.setAcao(acao);
        auditoria.setRecurso(recurso);
        auditoria.setDetalhes(detalhes);
        auditoria.setIdUsuario(idUsuario);
        auditoria.setEnderecoIp(enderecoIp);
        auditoria.setSucesso(sucesso);
        return auditoria;
    }
}
