package carshub.com.br.backend.models.dtos;


import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Usuario;

/**
 * Perfil publico de leitura (GET /perfil dos 4 atores).
 * REGRA 4.1: latitude e longitude NAO aparecem aqui (nem sao editaveis por
 * aqui) - a localizacao segue exclusiva dos endpoints dedicados
 * PUT .../localizacao e dos fluxos de GPS/endereco.
 *
 * Item 2: a reputacao do entusiasta (nota media dada pelos prestadores +
 * quantidade de avaliacoes) e exposta somente para perfis de ENTUSIASTA/
 * COLECIONADOR; nos demais perfis os campos vem nulos.
 */
public record PerfilUsuarioDTO(
        String nome,
        String telefone,
        String login,
        String urlFotoPerfil,
        Double notaMediaEntusiasta,
        Long quantidadeAvaliacoes) {

    public PerfilUsuarioDTO(Usuario user) {
        this(user.getNome(),
                user.getTelefone(),
                user.getLogin(),
                user.getUrlFotoPerfil(),
                user instanceof Entusiasta entusiasta ? entusiasta.getNotaMediaEntusiasta() : null,
                user instanceof Entusiasta entusiasta ? entusiasta.getQuantidadeAvaliacoes() : null);
    }
}
