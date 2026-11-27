package carshub.com.br.backend.models.dtos;


import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.Usuario;

/**
 * Perfil publico de leitura (GET /perfil dos 4 atores).
 * REGRA 4.1 (revisada): latitude e longitude APARECEM aqui para leitura
 * (entusiasta, mecanico e guincho; o organizador nao tem localizacao propria e
 * recebe null). A ESCRITA continua exclusiva do PUT .../localizacao e dos fluxos
 * de GPS/endereco (chamados, painel de disponiveis e proposta).
 * O frontend usa "latitude != null && longitude != null" para saber se existe
 * uma localizacao anterior reaproveitavel.
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
        Long quantidadeAvaliacoes,
        Double latitude,
        Double longitude,
        /* Endereco salvo junto com as coordenadas (so o colecionador/entusiasta tem; demais = null). */
        String enderecoFormatado) {

    public PerfilUsuarioDTO(Usuario user) {
        this(user.getNome(),
                user.getTelefone(),
                user.getLogin(),
                user.getUrlFotoPerfil(),
                user instanceof Entusiasta entusiasta ? entusiasta.getNotaMediaEntusiasta() : null,
                user instanceof Entusiasta entusiasta ? entusiasta.getQuantidadeAvaliacoes() : null,
                latitudeDe(user),
                longitudeDe(user),
                user instanceof Entusiasta entusiasta ? entusiasta.getEnderecoFormatado() : null);
    }

    private static Double latitudeDe(Usuario user) {
        if (user instanceof Entusiasta entusiasta) return entusiasta.getLatitude();
        if (user instanceof Mecanico mecanico) return mecanico.getLatitude();
        if (user instanceof Guincho guincho) return guincho.getLatitude();
        return null;
    }

    private static Double longitudeDe(Usuario user) {
        if (user instanceof Entusiasta entusiasta) return entusiasta.getLongitude();
        if (user instanceof Mecanico mecanico) return mecanico.getLongitude();
        if (user instanceof Guincho guincho) return guincho.getLongitude();
        return null;
    }
}
